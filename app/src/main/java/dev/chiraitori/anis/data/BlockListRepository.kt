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
        reloadActiveDomains()

        // Asynchronously check and pull down missing blocklist files from network
        CoroutineScope(Dispatchers.IO).launch {
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
        DefaultBlockLists.SOURCES.forEach { defaultSrc ->
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
        val newSet = mutableSetOf<String>()

        _sourcesFlow.value.filter { it.isEnabled }.forEach { src ->
            val file = File(blocklistDir, "${src.id}.txt")
            if (file.exists() && file.length() > 0L) {
                try {
                    file.forEachLine { line ->
                        val trimmed = line.trim().lowercase()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            newSet.add(trimmed)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BlockListRepo", "Error reading ${file.name}", e)
                }
            }
        }

        activeDomainsSet.clear()
        activeDomainsSet.addAll(newSet)

        QueryLogRepository.instance.updateActiveRulesCount(activeDomainsSet.size)
    }

    fun toggleList(id: String, enabled: Boolean) {
        val updated = _sourcesFlow.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        _sourcesFlow.value = updated
        saveSources()
        reloadActiveDomains()
    }

    fun enableAllBlockLists() {
        val updated = _sourcesFlow.value.map { it.copy(isEnabled = true) }
        _sourcesFlow.value = updated
        saveSources()
        reloadActiveDomains()
    }

    fun disableAllBlockLists() {
        val updated = _sourcesFlow.value.map { it.copy(isEnabled = false) }
        _sourcesFlow.value = updated
        saveSources()
        reloadActiveDomains()
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

    suspend fun updateAllLists() = withContext(Dispatchers.IO) {
        if (_isUpdatingFlow.value) return@withContext
        _isUpdatingFlow.value = true

        // Refresh remote list definitions from GitHub first
        syncRemoteSourcesIfDue(force = true)

        val currentList = _sourcesFlow.value
        val updatedList = mutableListOf<BlockListSource>()

        for (src in currentList) {
            _updateProgressFlow.value = "Updating ${src.name}..."
            val count = fetchAndSaveList(src)
            updatedList.add(
                src.copy(
                    ruleCount = if (count > 0) count else src.ruleCount,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }

        _sourcesFlow.value = updatedList
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

                val parsedDomains = mutableSetOf<String>()
                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var line: String? = reader.readLine()

                while (line != null) {
                    val parsed = CustomRuleParser.parseRule(line)
                    if (parsed != null && parsed.ruleType == RuleType.BLOCK && parsed.domain.isNotEmpty()) {
                        parsedDomains.add(parsed.domain)
                    }
                    line = reader.readLine()
                }

                if (parsedDomains.isNotEmpty()) {
                    val file = File(blocklistDir, "${source.id}.txt")
                    file.bufferedWriter().use { writer ->
                        parsedDomains.forEach { domain ->
                            writer.write(domain)
                            writer.newLine()
                        }
                    }
                    return parsedDomains.size
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
