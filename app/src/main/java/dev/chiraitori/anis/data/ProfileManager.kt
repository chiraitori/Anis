package dev.chiraitori.anis.data

import android.content.Context
import android.content.SharedPreferences
import dev.chiraitori.anis.data.model.ProfileType
import dev.chiraitori.anis.data.model.ProtectionProfile
import dev.chiraitori.anis.data.model.RuleCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ProfileManager(
    private val context: Context,
    private val blockListRepository: BlockListRepository,
    private val settingsRepository: SettingsRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("anis_profiles_prefs", Context.MODE_PRIVATE)

    private val _profilesFlow = MutableStateFlow<List<ProtectionProfile>>(emptyList())
    val profilesFlow: StateFlow<List<ProtectionProfile>> = _profilesFlow.asStateFlow()

    private val _activeProfileFlow = MutableStateFlow<ProtectionProfile>(DEFAULT_PROFILE)
    val activeProfileFlow: StateFlow<ProtectionProfile> = _activeProfileFlow.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val savedJson = prefs.getString(KEY_PROFILES, null)
        val list = mutableListOf<ProtectionProfile>()

        if (savedJson != null) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val filterIds = mutableSetOf<String>()
                    val fArray = obj.optJSONArray("enabledFilterIds")
                    if (fArray != null) {
                        for (j in 0 until fArray.length()) filterIds.add(fArray.getString(j))
                    }
                    list.add(
                        ProtectionProfile(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            profileType = try {
                                ProfileType.valueOf(obj.getString("profileType"))
                            } catch (_: Exception) {
                                ProfileType.DEFAULT
                            },
                            description = obj.optString("description", ""),
                            enabledFilterIds = filterIds,
                            safeSearchEnabled = obj.optBoolean("safeSearchEnabled", false),
                            youtubeRestrictedMode = obj.optBoolean("youtubeRestrictedMode", false),
                            isActive = obj.optBoolean("isActive", false)
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        // If empty or missing presets, seed them
        val defaultPresets = createDefaultPresets()
        val existingIds = list.map { it.id }.toSet()
        defaultPresets.forEach { preset ->
            if (preset.id !in existingIds) {
                list.add(preset)
            }
        }

        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, DEFAULT_PROFILE.id)
        val active = list.firstOrNull { it.id == activeId } ?: list.firstOrNull() ?: DEFAULT_PROFILE
        val markedList = list.map { it.copy(isActive = it.id == active.id) }

        _profilesFlow.value = markedList
        _activeProfileFlow.value = active
    }

    private fun saveProfiles() {
        val array = JSONArray()
        _profilesFlow.value.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("profileType", p.profileType.name)
                put("description", p.description)
                put("safeSearchEnabled", p.safeSearchEnabled)
                put("youtubeRestrictedMode", p.youtubeRestrictedMode)
                put("isActive", p.isActive)

                val fArray = JSONArray()
                p.enabledFilterIds.forEach { fArray.put(it) }
                put("enabledFilterIds", fArray)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    fun switchToProfile(profileId: String) {
        val target = _profilesFlow.value.firstOrNull { it.id == profileId } ?: return
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, target.id).apply()

        val updatedList = _profilesFlow.value.map { it.copy(isActive = it.id == target.id) }
        _profilesFlow.value = updatedList
        _activeProfileFlow.value = target.copy(isActive = true)
        saveProfiles()

        // Apply filter sources
        val allSources = blockListRepository.sourcesFlow.value
        allSources.forEach { src ->
            val shouldEnable = src.id in target.enabledFilterIds
            if (src.isEnabled != shouldEnable) {
                blockListRepository.toggleList(src.id, shouldEnable)
            }
        }

        // Apply SafeSearch and YouTube settings
        settingsRepository.safeSearchEnabled = target.safeSearchEnabled
        settingsRepository.youtubeRestrictedMode = target.youtubeRestrictedMode
    }

    fun updateProfile(profile: ProtectionProfile) {
        val updated = _profilesFlow.value.map { if (it.id == profile.id) profile else it }
        _profilesFlow.value = updated
        if (_activeProfileFlow.value.id == profile.id) {
            _activeProfileFlow.value = profile
            switchToProfile(profile.id)
        } else {
            saveProfiles()
        }
    }

    private fun createDefaultPresets(): List<ProtectionProfile> {
        val defaultFilterIds = setOf("adguard_base", "steven_black", "easylist", "easyprivacy", "peter_lowe")
        val strictFilterIds = setOf("adguard_base", "steven_black", "easylist", "easyprivacy", "peter_lowe", "fanboy_social", "adguard_tracking", "oem_telemetry", "urlhaus_malware")
        val familyFilterIds = setOf("adguard_base", "steven_black", "easylist", "urlhaus_malware", "steven_black_porn", "steven_black_gambling")
        val gamingFilterIds = setOf("adguard_base", "peter_lowe")

        return listOf(
            ProtectionProfile(
                id = "profile_default",
                name = "Standard Protection",
                profileType = ProfileType.DEFAULT,
                description = "Balanced ad & tracker blocking for everyday use",
                enabledFilterIds = defaultFilterIds,
                safeSearchEnabled = false,
                youtubeRestrictedMode = false,
                isActive = true
            ),
            ProtectionProfile(
                id = "profile_strict",
                name = "Strict Shield",
                profileType = ProfileType.STRICT,
                description = "Aggressive blocking: Ads, trackers, social widgets & OEM telemetry",
                enabledFilterIds = strictFilterIds,
                safeSearchEnabled = false,
                youtubeRestrictedMode = false,
                isActive = false
            ),
            ProtectionProfile(
                id = "profile_family",
                name = "Family Safe",
                profileType = ProfileType.FAMILY,
                description = "Enforces SafeSearch, YouTube restrictions, adult & gambling blocklists",
                enabledFilterIds = familyFilterIds,
                safeSearchEnabled = true,
                youtubeRestrictedMode = true,
                isActive = false
            ),
            ProtectionProfile(
                id = "profile_gaming",
                name = "Gaming & Speed",
                profileType = ProfileType.GAMING,
                description = "Minimal latency rules, only blocking intrusive ads",
                enabledFilterIds = gamingFilterIds,
                safeSearchEnabled = false,
                youtubeRestrictedMode = false,
                isActive = false
            )
        )
    }

    companion object {
        private const val KEY_PROFILES = "saved_protection_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

        val DEFAULT_PROFILE = ProtectionProfile(
            id = "profile_default",
            name = "Standard Protection",
            profileType = ProfileType.DEFAULT,
            description = "Balanced ad & tracker blocking",
            enabledFilterIds = setOf("adguard_base", "steven_black", "easylist", "easyprivacy", "peter_lowe"),
            safeSearchEnabled = false,
            youtubeRestrictedMode = false,
            isActive = true
        )

        @Volatile
        private var instance: ProfileManager? = null

        fun getInstance(
            context: Context,
            blockListRepository: BlockListRepository,
            settingsRepository: SettingsRepository
        ): ProfileManager {
            return instance ?: synchronized(this) {
                instance ?: ProfileManager(
                    context.applicationContext,
                    blockListRepository,
                    settingsRepository
                ).also { instance = it }
            }
        }
    }
}
