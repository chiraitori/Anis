package dev.chiraitori.anis.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dev.chiraitori.anis.data.model.BlockListSource
import dev.chiraitori.anis.data.model.RuleCategory
import dev.chiraitori.anis.data.model.RuleType
import dev.chiraitori.anis.vpn.CustomRuleParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class BlockListRepository(private val context: Context) {

    private val reloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reloadJob: Job? = null

    private val prefs: SharedPreferences =
        context.getSharedPreferences("anis_blocklists_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val blocklistDir = File(context.filesDir, "blocklists").apply { mkdirs() }

    private val _sourcesFlow = MutableStateFlow<List<BlockListSource>>(emptyList())
    val sourcesFlow: StateFlow<List<BlockListSource>> = _sourcesFlow.asStateFlow()

    private val _isUpdatingFlow = MutableStateFlow(false)
    val isUpdatingFlow: StateFlow<Boolean> = _isUpdatingFlow.asStateFlow()

    private val _updateProgressFlow = MutableStateFlow("")
    val updateProgressFlow: StateFlow<String> = _updateProgressFlow.asStateFlow()

    // Thread-safe in-memory cache of all active blocked domains
    private val activeDomainsSet = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    init {
        loadSources()

        // Asynchronously load active domains from storage and sync without blocking UI startup
        CoroutineScope(Dispatchers.IO).launch {
            reloadActiveDomains()
            ensureEnabledListsDownloaded()
            syncRemoteSourcesIfDue()
        }
    }

    private suspend fun ensureEnabledListsDownloaded() = withContext(Dispatchers.IO) {
        val enabledSources = _sourcesFlow.value.filter { it.isEnabled }
        var hasNewDownloads = false

        for (src in enabledSources) {
            val file = File(blocklistDir, "${src.id}.txt")
            if (!file.exists() || file.length() == 0L) {
                Log.i("BlockListRepo", "Pulling down initial blocklist: ${src.name} (${src.url})")
                val count = fetchAndSaveList(src)
                if (count > 0) {
                    hasNewDownloads = true
                    val updated = _sourcesFlow.value.map {
                        if (it.id == src.id) it.copy(ruleCount = count, lastUpdated = System.currentTimeMillis()) else it
                    }
                    _sourcesFlow.value = updated
                    saveSources()
                }
            }
        }

        if (hasNewDownloads) {
            reloadActiveDomains()
            Log.i("BlockListRepo", "Initial blocklists pulled down. Active rules: ${activeDomainsSet.size}")
        }
    }

    private fun loadSources() {
        val savedJson = prefs.getString(KEY_SOURCES, null)
        val sources = mutableListOf<BlockListSource>()

        if (savedJson != null) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    sources.add(
                        BlockListSource(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            url = obj.getString("url"),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            ruleCount = obj.optInt("ruleCount", 0),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                            category = try {
                                RuleCategory.valueOf(obj.optString("category", RuleCategory.ADS.name))
                            } catch (e: Exception) {
                                RuleCategory.ADS
                            },
                            isCustom = obj.optBoolean("isCustom", false)
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("BlockListRepo", "Failed to parse saved blocklists", e)
            }
        }

        // Merge defaults if not present
        val existingIds = sources.map { it.id }.toSet()
        val defaultSources = BlockListSourceLoader.loadFromAssets(context)
        defaultSources.forEach { defaultSrc ->
            if (defaultSrc.id !in existingIds) {
                sources.add(defaultSrc)
            }
        }

        _sourcesFlow.value = sources
    }

    private fun saveSources() {
        val array = JSONArray()
        _sourcesFlow.value.forEach { src ->
            val obj = JSONObject().apply {
                put("id", src.id)
                put("name", src.name)
                put("description", src.description)
                put("url", src.url)
                put("isEnabled", src.isEnabled)
                put("ruleCount", src.ruleCount)
                put("lastUpdated", src.lastUpdated)
                put("category", src.category.name)
                put("isCustom", src.isCustom)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SOURCES, array.toString()).apply()
    }

    /**
     * Pulls the latest sources.json from GitHub if 7 days have elapsed or if forced.
     */
    suspend fun syncRemoteSourcesIfDue(force: Boolean = false) = withContext(Dispatchers.IO) {
        val lastSync = prefs.getLong(KEY_LAST_REMOTE_SYNC_TIME, 0L)
        val now = System.currentTimeMillis()
        if (!force && (now - lastSync) < SEVEN_DAYS_MS && _sourcesFlow.value.isNotEmpty()) {
            return@withContext
        }

        try {
            val request = Request.Builder()
                .url(DEFAULT_REMOTE_SOURCES_URL)
                .header("User-Agent", "Mozilla/5.0 AnisDNS/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext
                val body = response.body?.string() ?: return@withContext
                val root = JSONObject(body)
                val sourcesArray = root.optJSONArray("sources") ?: return@withContext

                val remoteSources = mutableListOf<BlockListSource>()
                for (i in 0 until sourcesArray.length()) {
                    val obj = sourcesArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val cat = try {
                        RuleCategory.valueOf(obj.optString("category", RuleCategory.ADS.name))
                    } catch (e: Exception) {
                        RuleCategory.ADS
                    }

                    remoteSources.add(
                        BlockListSource(
                            id = id,
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            url = obj.getString("url"),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            ruleCount = obj.optInt("ruleCount", 0),
                            lastUpdated = now,
                            category = cat,
                            isCustom = false
                        )
                    )
                }

                if (remoteSources.isNotEmpty()) {
                    val currentMap = _sourcesFlow.value.associateBy { it.id }.toMutableMap()
                    val merged = mutableListOf<BlockListSource>()

                    // Merge remote sources, preserving user's isEnabled preference
                    remoteSources.forEach { remote ->
                        val existing = currentMap[remote.id]
                        if (existing != null) {
                            merged.add(
                                remote.copy(
                                    isEnabled = existing.isEnabled,
                                    ruleCount = if (existing.ruleCount > 0) existing.ruleCount else remote.ruleCount
                                )
                            )
                            currentMap.remove(remote.id)
                        } else {
                            merged.add(remote)
                        }
                    }

                    // Keep custom lists added by user
                    currentMap.values.forEach { remaining ->
                        if (remaining.isCustom) {
                            merged.add(remaining)
                        }
                    }

                    _sourcesFlow.value = merged
                    saveSources()
                    prefs.edit().putLong(KEY_LAST_REMOTE_SYNC_TIME, now).apply()
                    Log.d("BlockListRepo", "Synced ${merged.size} sources from GitHub repository.")
                }
            }
        } catch (e: Exception) {
            Log.w("BlockListRepo", "Remote sources sync skipped: ${e.message}")
        }
    }

    fun getActiveBlockedDomains(): Set<String> {
        return activeDomainsSet
    }

    fun isDomainBlocked(domain: String): Boolean {
        val clean = domain.lowercase().trim()
        if (activeDomainsSet.contains(clean)) return true
        var dotIdx = clean.indexOf('.')
        while (dotIdx != -1) {
            val parent = clean.substring(dotIdx + 1)
            if (activeDomainsSet.contains(parent)) return true
            dotIdx = clean.indexOf('.', dotIdx + 1)
        }
        return false
    }

    fun reloadActiveDomains() {
        val enabledSources = _sourcesFlow.value.filter { it.isEnabled }

        synchronized(activeDomainsSet) {
            activeDomainsSet.clear()
            enabledSources.forEach { src ->
                val file = File(blocklistDir, "${src.id}.txt")
                if (file.exists() && file.length() > 0L) {
                    try {
                        file.forEachLine { line ->
                            val trimmed = line.trim().lowercase()
                            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                                activeDomainsSet.add(trimmed)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BlockListRepo", "Error reading ${file.name}", e)
                    }
                }
            }
        }

        QueryLogRepository.instance.updateActiveRulesCount(activeDomainsSet.size)
        System.gc()
    }

    fun scheduleReloadActiveDomains() {
        reloadJob?.cancel()
        reloadJob = reloadScope.launch {
            reloadActiveDomains()
        }
    }

    fun toggleList(id: String, enabled: Boolean) {
        val updated = _sourcesFlow.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        _sourcesFlow.value = updated
        saveSources()
        scheduleReloadActiveDomains()
    }

    fun enableAllBlockLists() {
        val updated = _sourcesFlow.value.map { it.copy(isEnabled = true) }
        _sourcesFlow.value = updated
        saveSources()
        scheduleReloadActiveDomains()
    }

    fun disableAllBlockLists() {
        val updated = _sourcesFlow.value.map { it.copy(isEnabled = false) }
        _sourcesFlow.value = updated
        saveSources()
        scheduleReloadActiveDomains()
    }

    fun addCustomList(name: String, url: String, category: RuleCategory = RuleCategory.CUSTOM): BlockListSource {
        val id = "custom_${System.currentTimeMillis()}"
        val newSrc = BlockListSource(
            id = id,
            name = name,
            description = "Custom blocklist from $url",
            url = url.trim(),
            isEnabled = true,
            ruleCount = 0,
            lastUpdated = System.currentTimeMillis(),
            category = category,
            isCustom = true
        )
        _sourcesFlow.value = _sourcesFlow.value + newSrc
        saveSources()
        return newSrc
    }

    fun removeCustomList(id: String) {
        val file = File(blocklistDir, "$id.txt")
        if (file.exists()) file.delete()

        _sourcesFlow.value = _sourcesFlow.value.filterNot { it.id == id }
        saveSources()
        reloadActiveDomains()
    }

    fun exportBackupJson(): JSONArray = JSONArray().apply {
        _sourcesFlow.value.forEach { source ->
            put(JSONObject().apply {
                put("id", source.id)
                put("name", source.name)
                put("url", source.url)
                put("isEnabled", source.isEnabled)
                put("category", source.category.name)
                put("isCustom", source.isCustom)
            })
        }
    }

    fun importBackupJson(array: JSONArray): Boolean {
        return try {
            val currentDefaults = _sourcesFlow.value.filterNot { it.isCustom }
            val enabledById = mutableMapOf<String, Boolean>()
            val restoredCustom = mutableListOf<BlockListSource>()

            for (index in 0 until minOf(array.length(), 250)) {
                val item = array.getJSONObject(index)
                val id = item.optString("id")
                if (item.optBoolean("isCustom", false)) {
                    val url = item.getString("url").trim()
                    if (!url.startsWith("https://") && !url.startsWith("http://")) continue
                    val safeId = id.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,80}")) }
                        ?: "custom_${System.currentTimeMillis()}_$index"
                    restoredCustom += BlockListSource(
                        id = safeId,
                        name = item.optString("name", "Custom blocklist").take(100),
                        description = "Custom blocklist from $url",
                        url = url,
                        isEnabled = item.optBoolean("isEnabled", true),
                        category = runCatching {
                            RuleCategory.valueOf(item.optString("category", RuleCategory.CUSTOM.name))
                        }.getOrDefault(RuleCategory.CUSTOM),
                        isCustom = true
                    )
                } else if (id.isNotBlank()) {
                    enabledById[id] = item.optBoolean("isEnabled", true)
                }
            }

            _sourcesFlow.value = currentDefaults.map { source ->
                source.copy(isEnabled = enabledById[source.id] ?: source.isEnabled)
            } + restoredCustom.distinctBy { it.id }
            saveSources()
            reloadScope.launch {
                ensureEnabledListsDownloaded()
                reloadActiveDomains()
            }
            true
        } catch (error: Exception) {
            Log.e("BlockListRepo", "Failed to restore blocklist settings", error)
            false
        }
    }

    suspend fun updateAllLists(onlyEnabled: Boolean = true) = withContext(Dispatchers.IO) {
        if (_isUpdatingFlow.value) return@withContext
        _isUpdatingFlow.value = true

        // Refresh remote list definitions from GitHub first
        syncRemoteSourcesIfDue(force = true)

        val currentList = _sourcesFlow.value
        val targets = if (onlyEnabled) currentList.filter { it.isEnabled } else currentList
        val updatedMap = currentList.associateBy { it.id }.toMutableMap()

        for (src in targets) {
            _updateProgressFlow.value = "Updating ${src.name}..."
            val count = fetchAndSaveList(src)
            if (count > 0) {
                updatedMap[src.id] = src.copy(
                    ruleCount = count,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }

        _sourcesFlow.value = updatedMap.values.toList()
        saveSources()
        reloadActiveDomains()

        _updateProgressFlow.value = "Done! Protection up to date."
        _isUpdatingFlow.value = false
    }

    suspend fun updateList(id: String) = withContext(Dispatchers.IO) {
        val src = _sourcesFlow.value.firstOrNull { it.id == id } ?: return@withContext
        _updateProgressFlow.value = "Updating ${src.name}..."
        val count = fetchAndSaveList(src)
        val updated = _sourcesFlow.value.map {
            if (it.id == id) it.copy(
                ruleCount = if (count > 0) count else src.ruleCount,
                lastUpdated = System.currentTimeMillis()
            ) else it
        }
        _sourcesFlow.value = updated
        saveSources()
        reloadActiveDomains()
    }

    private fun fetchAndSaveList(source: BlockListSource): Int {
        try {
            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", "Mozilla/5.0 AnisDNS/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return 0
                val body = response.body ?: return 0

                val tempFile = File(blocklistDir, "${source.id}.tmp")
                val finalFile = File(blocklistDir, "${source.id}.txt")
                var count = 0

                tempFile.bufferedWriter().use { writer ->
                    body.byteStream().bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val parsed = CustomRuleParser.parseRule(line)
                            if (parsed != null && parsed.ruleType == RuleType.BLOCK && parsed.domain.isNotEmpty()) {
                                writer.write(parsed.domain)
                                writer.newLine()
                                count++
                            }
                        }
                    }
                }

                if (count > 0) {
                    if (finalFile.exists()) finalFile.delete()
                    tempFile.renameTo(finalFile)
                    return count
                } else {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("BlockListRepo", "Failed to download list ${source.name}", e)
        }
        return 0
    }

    companion object {
        private const val KEY_SOURCES = "saved_sources"
        private const val KEY_LAST_REMOTE_SYNC_TIME = "last_remote_sources_sync_time"
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000L
        const val DEFAULT_REMOTE_SOURCES_URL =
            "https://raw.githubusercontent.com/chiraitori/Anis/main/sources.json"

        @Volatile
        private var instance: BlockListRepository? = null

        fun getInstance(context: Context): BlockListRepository {
            return instance ?: synchronized(this) {
                instance ?: BlockListRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
