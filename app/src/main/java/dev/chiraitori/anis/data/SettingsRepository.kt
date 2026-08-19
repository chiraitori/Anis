package dev.chiraitori.anis.data

import android.content.Context
import android.content.SharedPreferences
import dev.chiraitori.anis.data.model.AppLanguage
import dev.chiraitori.anis.data.model.AutoUpdateFrequency
import dev.chiraitori.anis.data.model.CustomDnsRule
import dev.chiraitori.anis.data.model.DefaultDnsProviders
import dev.chiraitori.anis.data.model.DnsCategory
import dev.chiraitori.anis.data.model.DnsProtocol
import dev.chiraitori.anis.data.model.DnsResponseType
import dev.chiraitori.anis.data.model.LogRetention
import dev.chiraitori.anis.data.model.ProtectionMode
import dev.chiraitori.anis.data.model.ThemeMode
import dev.chiraitori.anis.data.model.UpstreamDnsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("anis_settings_prefs", Context.MODE_PRIVATE)

    private val _upstreamDnsFlow = MutableStateFlow(loadUpstreamDns())
    val upstreamDnsFlow: StateFlow<UpstreamDnsProvider> = _upstreamDnsFlow.asStateFlow()

    private val _dnsProtocolFlow = MutableStateFlow(loadDnsProtocol())
    val dnsProtocolFlow: StateFlow<DnsProtocol> = _dnsProtocolFlow.asStateFlow()

    private val _protectionModeFlow = MutableStateFlow(loadProtectionMode())
    val protectionModeFlow: StateFlow<ProtectionMode> = _protectionModeFlow.asStateFlow()

    private val _dnsResponseTypeFlow = MutableStateFlow(loadDnsResponseType())
    val dnsResponseTypeFlow: StateFlow<DnsResponseType> = _dnsResponseTypeFlow.asStateFlow()

    private val _autoUpdateFrequencyFlow = MutableStateFlow(loadAutoUpdateFrequency())
    val autoUpdateFrequencyFlow: StateFlow<AutoUpdateFrequency> = _autoUpdateFrequencyFlow.asStateFlow()

    private val _autoUpdateWifiOnlyFlow = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE_WIFI_ONLY, true))
    val autoUpdateWifiOnlyFlow: StateFlow<Boolean> = _autoUpdateWifiOnlyFlow.asStateFlow()

    private val _autoUpdateNotificationFlow = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE_NOTIFICATION, true))
    val autoUpdateNotificationFlow: StateFlow<Boolean> = _autoUpdateNotificationFlow.asStateFlow()

    private val _autoReconnectFlow = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RECONNECT, true))
    val autoReconnectFlow: StateFlow<Boolean> = _autoReconnectFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(loadThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    private val _appLanguageFlow = MutableStateFlow(loadAppLanguage())
    val appLanguageFlow: StateFlow<AppLanguage> = _appLanguageFlow.asStateFlow()

    private val _logRetentionFlow = MutableStateFlow(loadLogRetention())
    val logRetentionFlow: StateFlow<LogRetention> = _logRetentionFlow.asStateFlow()

    private val _hapticsEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))
    val hapticsEnabledFlow: StateFlow<Boolean> = _hapticsEnabledFlow.asStateFlow()

    private val _whitelistFlow = MutableStateFlow(loadStringSet(KEY_WHITELIST))
    val whitelistFlow: StateFlow<Set<String>> = _whitelistFlow.asStateFlow()

    private val _blacklistFlow = MutableStateFlow(loadStringSet(KEY_BLACKLIST))
    val blacklistFlow: StateFlow<Set<String>> = _blacklistFlow.asStateFlow()

    private val _firewallBlockedAppsFlow = MutableStateFlow(loadStringSet(KEY_FIREWALL_BLOCKED))
    val firewallBlockedAppsFlow: StateFlow<Set<String>> = _firewallBlockedAppsFlow.asStateFlow()

    private val _whitelistedAppsFlow = MutableStateFlow(loadStringSet(KEY_WHITELISTED_APPS))
    val whitelistedAppsFlow: StateFlow<Set<String>> = _whitelistedAppsFlow.asStateFlow()

    private val _customRulesFlow = MutableStateFlow(loadCustomRules())
    val customRulesFlow: StateFlow<List<CustomDnsRule>> = _customRulesFlow.asStateFlow()

    private val _trustedSsidsFlow = MutableStateFlow(loadStringSet(KEY_TRUSTED_SSIDS))
    val trustedSsidsFlow: StateFlow<Set<String>> = _trustedSsidsFlow.asStateFlow()

    private val _pauseOnTrustedEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_ON_TRUSTED, false))
    val pauseOnTrustedEnabledFlow: StateFlow<Boolean> = _pauseOnTrustedEnabledFlow.asStateFlow()

    private val _isPausedByTrustedFlow = MutableStateFlow(prefs.getBoolean(KEY_IS_PAUSED_BY_TRUSTED, false))
    val isPausedByTrustedFlow: StateFlow<Boolean> = _isPausedByTrustedFlow.asStateFlow()

    private val _isCaInstalledFlow = MutableStateFlow(prefs.getBoolean(KEY_CA_INSTALLED, false))
    val isCaInstalledFlow: StateFlow<Boolean> = _isCaInstalledFlow.asStateFlow()

    private val _isCaDismissedFlow = MutableStateFlow(prefs.getBoolean(KEY_CA_DISMISSED, false))
    val isCaDismissedFlow: StateFlow<Boolean> = _isCaDismissedFlow.asStateFlow()

    var isCaInstalled: Boolean
        get() = _isCaInstalledFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_CA_INSTALLED, value).apply()
            _isCaInstalledFlow.value = value
        }

    var isCaDismissed: Boolean
        get() = _isCaDismissedFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_CA_DISMISSED, value).apply()
            _isCaDismissedFlow.value = value
        }

    private val _httpsFilteringEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_HTTPS_FILTERING, false))
    val httpsFilteringEnabledFlow: StateFlow<Boolean> = _httpsFilteringEnabledFlow.asStateFlow()

    var httpsFilteringEnabled: Boolean
        get() = _httpsFilteringEnabledFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_HTTPS_FILTERING, value).apply()
            _httpsFilteringEnabledFlow.value = value
        }

    private fun loadStringSet(key: String): Set<String> {
        val json = prefs.getString(key, null) ?: return emptySet()
        return try {
            val array = JSONArray(json)
            val set = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                set.add(array.getString(i).trim())
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun saveStringSet(key: String, set: Set<String>) {
        val array = JSONArray()
        set.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun loadDnsProtocol(): DnsProtocol {
        val name = prefs.getString(KEY_DNS_PROTOCOL, DnsProtocol.DOH.name)
        return try {
            DnsProtocol.valueOf(name ?: DnsProtocol.DOH.name)
        } catch (e: Exception) {
            DnsProtocol.DOH
        }
    }

    fun setDnsProtocol(protocol: DnsProtocol) {
        prefs.edit().putString(KEY_DNS_PROTOCOL, protocol.name).apply()
        _dnsProtocolFlow.value = protocol
    }

    private fun loadProtectionMode(): ProtectionMode {
        val name = prefs.getString(KEY_PROTECTION_MODE, ProtectionMode.LOCAL_VPN.name)
        return try {
            ProtectionMode.valueOf(name ?: ProtectionMode.LOCAL_VPN.name)
        } catch (e: Exception) {
            ProtectionMode.LOCAL_VPN
        }
    }

    fun setProtectionMode(mode: ProtectionMode) {
        prefs.edit().putString(KEY_PROTECTION_MODE, mode.name).apply()
        _protectionModeFlow.value = mode
    }

    private fun loadDnsResponseType(): DnsResponseType {
        val name = prefs.getString(KEY_DNS_RESPONSE_TYPE, DnsResponseType.ZERO_IP.name)
        return try {
            DnsResponseType.valueOf(name ?: DnsResponseType.ZERO_IP.name)
        } catch (e: Exception) {
            DnsResponseType.ZERO_IP
        }
    }

    fun setDnsResponseType(type: DnsResponseType) {
        prefs.edit().putString(KEY_DNS_RESPONSE_TYPE, type.name).apply()
        _dnsResponseTypeFlow.value = type
    }

    private fun loadAutoUpdateFrequency(): AutoUpdateFrequency {
        val name = prefs.getString(KEY_AUTO_UPDATE_FREQUENCY, AutoUpdateFrequency.DAILY.name)
        return try {
            AutoUpdateFrequency.valueOf(name ?: AutoUpdateFrequency.DAILY.name)
        } catch (e: Exception) {
            AutoUpdateFrequency.DAILY
        }
    }

    fun setAutoUpdateFrequency(frequency: AutoUpdateFrequency) {
        prefs.edit().putString(KEY_AUTO_UPDATE_FREQUENCY, frequency.name).apply()
        _autoUpdateFrequencyFlow.value = frequency
    }

    fun setAutoUpdateWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_WIFI_ONLY, wifiOnly).apply()
        _autoUpdateWifiOnlyFlow.value = wifiOnly
    }

    fun setAutoUpdateNotification(notify: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_NOTIFICATION, notify).apply()
        _autoUpdateNotificationFlow.value = notify
    }

    fun setAutoReconnect(reconnect: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, reconnect).apply()
        _autoReconnectFlow.value = reconnect
    }

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeModeFlow.value = mode
    }

    private fun loadAppLanguage(): AppLanguage {
        val name = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.name)
        return try {
            AppLanguage.valueOf(name ?: AppLanguage.SYSTEM.name)
        } catch (e: Exception) {
            AppLanguage.SYSTEM
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _appLanguageFlow.value = language
    }

    private fun loadLogRetention(): LogRetention {
        val name = prefs.getString(KEY_LOG_RETENTION, LogRetention.SEVEN_DAYS.name)
        return try {
            LogRetention.valueOf(name ?: LogRetention.SEVEN_DAYS.name)
        } catch (e: Exception) {
            LogRetention.SEVEN_DAYS
        }
    }

    fun setLogRetention(retention: LogRetention) {
        prefs.edit().putString(KEY_LOG_RETENTION, retention.name).apply()
        _logRetentionFlow.value = retention
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
        _hapticsEnabledFlow.value = enabled
    }

    private fun loadUpstreamDns(): UpstreamDnsProvider {
        val dnsId = prefs.getString(KEY_UPSTREAM_DNS_ID, DefaultDnsProviders.CLOUDFLARE.id)
        val defaultMatch = DefaultDnsProviders.ALL.firstOrNull { it.id == dnsId }
        if (defaultMatch != null) return defaultMatch

        val customName = prefs.getString(KEY_CUSTOM_DNS_NAME, "Custom DNS") ?: "Custom DNS"
        val customIp = prefs.getString(KEY_CUSTOM_DNS_PRIMARY, "1.1.1.1") ?: "1.1.1.1"
        val customSec = prefs.getString(KEY_CUSTOM_DNS_SECONDARY, "") ?: ""
        val customDoh = prefs.getString(KEY_CUSTOM_DNS_DOH, null)
        return UpstreamDnsProvider(
            id = "custom",
            name = customName,
            category = DnsCategory.CUSTOM,
            description = "Custom DNS ($customIp)",
            primaryIp = customIp,
            secondaryIp = customSec,
            dohUrl = customDoh,
            isCustom = true
        )
    }

    fun setUpstreamDns(provider: UpstreamDnsProvider) {
        prefs.edit().apply {
            putString(KEY_UPSTREAM_DNS_ID, provider.id)
            if (provider.isCustom) {
                putString(KEY_CUSTOM_DNS_NAME, provider.name)
                putString(KEY_CUSTOM_DNS_PRIMARY, provider.primaryIp)
                putString(KEY_CUSTOM_DNS_SECONDARY, provider.secondaryIp)
                putString(KEY_CUSTOM_DNS_DOH, provider.dohUrl)
            }
        }.apply()
        _upstreamDnsFlow.value = provider
    }

    fun setCustomUpstreamDns(name: String, primaryIp: String, secondaryIp: String = "", dohUrl: String? = null) {
        val provider = UpstreamDnsProvider(
            id = "custom",
            name = name,
            category = DnsCategory.CUSTOM,
            description = "Custom DNS ($primaryIp)",
            primaryIp = primaryIp,
            secondaryIp = secondaryIp,
            dohUrl = dohUrl,
            isCustom = true
        )
        setUpstreamDns(provider)
    }

    private fun loadCustomRules(): List<CustomDnsRule> {
        val json = prefs.getString(KEY_CUSTOM_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<CustomDnsRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CustomDnsRule(
                        id = obj.optString("id", "rule_$i"),
                        domain = obj.getString("domain"),
                        targetIp = obj.getString("targetIp"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomRule(domain: String, targetIp: String) {
        val newRule = CustomDnsRule(
            domain = domain.trim().lowercase(),
            targetIp = targetIp.trim(),
            isEnabled = true
        )
        val updated = _customRulesFlow.value + newRule
        saveCustomRules(updated)
    }

    fun removeCustomRule(id: String) {
        val updated = _customRulesFlow.value.filterNot { it.id == id }
        saveCustomRules(updated)
    }

    private fun saveCustomRules(rules: List<CustomDnsRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("domain", rule.domain)
                put("targetIp", rule.targetIp)
                put("isEnabled", rule.isEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_RULES, array.toString()).apply()
        _customRulesFlow.value = rules
    }

    fun addWhitelistDomain(domain: String) {
        val clean = domain.trim().lowercase()
        if (clean.isNotBlank()) {
            val updated = _whitelistFlow.value + clean
            saveStringSet(KEY_WHITELIST, updated)
            _whitelistFlow.value = updated
        }
    }

    fun removeWhitelistDomain(domain: String) {
        val clean = domain.trim().lowercase()
        val updated = _whitelistFlow.value - clean
        saveStringSet(KEY_WHITELIST, updated)
        _whitelistFlow.value = updated
    }

    fun addBlacklistDomain(domain: String) {
        val clean = domain.trim().lowercase()
        if (clean.isNotBlank()) {
            val updated = _blacklistFlow.value + clean
            saveStringSet(KEY_BLACKLIST, updated)
            _blacklistFlow.value = updated
        }
    }

    fun removeBlacklistDomain(domain: String) {
        val clean = domain.trim().lowercase()
        val updated = _blacklistFlow.value - clean
        saveStringSet(KEY_BLACKLIST, updated)
        _blacklistFlow.value = updated
    }

    fun setAppBlockedInFirewall(packageName: String, blocked: Boolean) {
        val current = _firewallBlockedAppsFlow.value.toMutableSet()
        if (blocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        saveStringSet(KEY_FIREWALL_BLOCKED, current)
        _firewallBlockedAppsFlow.value = current
    }

    fun setAllAppsBlockedInFirewall(packageNames: Set<String>, blocked: Boolean) {
        val current = _firewallBlockedAppsFlow.value.toMutableSet()
        if (blocked) {
            current.addAll(packageNames)
        } else {
            current.removeAll(packageNames)
        }
        saveStringSet(KEY_FIREWALL_BLOCKED, current)
        _firewallBlockedAppsFlow.value = current
    }

    fun toggleAppBypassVpn(packageName: String, isBypassed: Boolean) {
        val current = _whitelistedAppsFlow.value.toMutableSet()
        if (isBypassed) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        saveStringSet(KEY_WHITELISTED_APPS, current)
        _whitelistedAppsFlow.value = current
    }

    // Trusted Wi-Fi Networks
    fun addTrustedSsid(ssid: String) {
        val clean = ssid.trim().removeSurrounding("\"")
        if (clean.isNotEmpty()) {
            val updated = _trustedSsidsFlow.value + clean
            saveStringSet(KEY_TRUSTED_SSIDS, updated)
            _trustedSsidsFlow.value = updated
        }
    }

    fun removeTrustedSsid(ssid: String) {
        val clean = ssid.trim().removeSurrounding("\"")
        val updated = _trustedSsidsFlow.value - clean
        saveStringSet(KEY_TRUSTED_SSIDS, updated)
        _trustedSsidsFlow.value = updated
    }

    fun setPauseOnTrustedEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_ON_TRUSTED, enabled).apply()
        _pauseOnTrustedEnabledFlow.value = enabled
    }

    fun setPausedByTrusted(paused: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PAUSED_BY_TRUSTED, paused).apply()
        _isPausedByTrustedFlow.value = paused
    }

    var safeSearchEnabled: Boolean
        get() = prefs.getBoolean(KEY_SAFE_SEARCH, false)
        set(value) = prefs.edit().putBoolean(KEY_SAFE_SEARCH, value).apply()

    var youtubeRestrictedMode: Boolean
        get() = prefs.getBoolean(KEY_YOUTUBE_RESTRICTED, false)
        set(value) = prefs.edit().putBoolean(KEY_YOUTUBE_RESTRICTED, value).apply()

    var startOnBoot: Boolean
        get() = prefs.getBoolean(KEY_START_ON_BOOT, false)
        set(value) = prefs.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    // Full JSON Export & Import (inspired by BlockAds SettingsBackup)
    fun exportBackupJson(): String {
        val root = JSONObject().apply {
            put("version", 2)
            put("timestamp", System.currentTimeMillis())
            put("upstreamDnsId", _upstreamDnsFlow.value.id)
            put("dnsProtocol", _dnsProtocolFlow.value.name)
            put("safeSearchEnabled", safeSearchEnabled)
            put("youtubeRestricted", youtubeRestrictedMode)
            put("pauseOnTrusted", _pauseOnTrustedEnabledFlow.value)
            put("startOnBoot", startOnBoot)

            val wlArray = JSONArray()
            _whitelistFlow.value.forEach { wlArray.put(it) }
            put("whitelist", wlArray)

            val blArray = JSONArray()
            _blacklistFlow.value.forEach { blArray.put(it) }
            put("blacklist", blArray)

            val fwArray = JSONArray()
            _firewallBlockedAppsFlow.value.forEach { fwArray.put(it) }
            put("firewallBlocked", fwArray)

            val bpArray = JSONArray()
            _whitelistedAppsFlow.value.forEach { bpArray.put(it) }
            put("bypassedApps", bpArray)

            val ssidsArray = JSONArray()
            _trustedSsidsFlow.value.forEach { ssidsArray.put(it) }
            put("trustedSsids", ssidsArray)

            val rulesArray = JSONArray()
            _customRulesFlow.value.forEach { rule ->
                val rObj = JSONObject().apply {
                    put("id", rule.id)
                    put("domain", rule.domain)
                    put("targetIp", rule.targetIp)
                    put("isEnabled", rule.isEnabled)
                }
                rulesArray.put(rObj)
            }
            put("customRules", rulesArray)
        }
        return root.toString(2)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val dnsId = root.optString("upstreamDnsId", DefaultDnsProviders.CLOUDFLARE.id)
            val provider = DefaultDnsProviders.ALL.firstOrNull { it.id == dnsId } ?: DefaultDnsProviders.CLOUDFLARE
            setUpstreamDns(provider)

            if (root.has("safeSearchEnabled")) {
                safeSearchEnabled = root.getBoolean("safeSearchEnabled")
            }
            if (root.has("youtubeRestricted")) {
                youtubeRestrictedMode = root.getBoolean("youtubeRestricted")
            }
            if (root.has("startOnBoot")) {
                startOnBoot = root.getBoolean("startOnBoot")
            }
            if (root.has("pauseOnTrusted")) {
                setPauseOnTrustedEnabled(root.getBoolean("pauseOnTrusted"))
            }

            if (root.has("whitelist")) {
                val array = root.getJSONArray("whitelist")
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) set.add(array.getString(i))
                saveStringSet(KEY_WHITELIST, set)
                _whitelistFlow.value = set
            }

            if (root.has("blacklist")) {
                val array = root.getJSONArray("blacklist")
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) set.add(array.getString(i))
                saveStringSet(KEY_BLACKLIST, set)
                _blacklistFlow.value = set
            }

            if (root.has("firewallBlocked")) {
                val array = root.getJSONArray("firewallBlocked")
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) set.add(array.getString(i))
                saveStringSet(KEY_FIREWALL_BLOCKED, set)
                _firewallBlockedAppsFlow.value = set
            }

            if (root.has("bypassedApps")) {
                val array = root.getJSONArray("bypassedApps")
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) set.add(array.getString(i))
                saveStringSet(KEY_WHITELISTED_APPS, set)
                _whitelistedAppsFlow.value = set
            }

            if (root.has("trustedSsids")) {
                val array = root.getJSONArray("trustedSsids")
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) set.add(array.getString(i))
                saveStringSet(KEY_TRUSTED_SSIDS, set)
                _trustedSsidsFlow.value = set
            }

            if (root.has("customRules")) {
                val array = root.getJSONArray("customRules")
                val rules = mutableListOf<CustomDnsRule>()
                for (i in 0 until array.length()) {
                    val rObj = array.getJSONObject(i)
                    rules.add(
                        CustomDnsRule(
                            id = rObj.optString("id", "rule_$i"),
                            domain = rObj.getString("domain"),
                            targetIp = rObj.getString("targetIp"),
                            isEnabled = rObj.optBoolean("isEnabled", true)
                        )
                    )
                }
                saveCustomRules(rules)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_UPSTREAM_DNS_ID = "upstream_dns_id"
        private const val KEY_DNS_PROTOCOL = "dns_protocol"
        private const val KEY_CUSTOM_DNS_NAME = "custom_dns_name"
        private const val KEY_CUSTOM_DNS_PRIMARY = "custom_dns_primary"
        private const val KEY_CUSTOM_DNS_SECONDARY = "custom_dns_secondary"
        private const val KEY_CUSTOM_DNS_DOH = "custom_dns_doh"
        private const val KEY_WHITELIST = "whitelist_domains"
        private const val KEY_BLACKLIST = "blacklist_domains"
        private const val KEY_FIREWALL_BLOCKED = "firewall_blocked_packages"
        private const val KEY_WHITELISTED_APPS = "whitelisted_bypassed_apps"
        private const val KEY_CUSTOM_RULES = "custom_dns_rules"
        private const val KEY_TRUSTED_SSIDS = "trusted_wifi_ssids"
        private const val KEY_PAUSE_ON_TRUSTED = "pause_on_trusted_wifi"
        private const val KEY_IS_PAUSED_BY_TRUSTED = "is_currently_paused_by_trusted"
        private const val KEY_SAFE_SEARCH = "safe_search_enabled"
        private const val KEY_YOUTUBE_RESTRICTED = "youtube_restricted_mode"
        private const val KEY_START_ON_BOOT = "start_on_boot"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_PROTECTION_MODE = "protection_mode"
        private const val KEY_DNS_RESPONSE_TYPE = "dns_response_type"
        private const val KEY_AUTO_UPDATE_FREQUENCY = "auto_update_frequency"
        private const val KEY_AUTO_UPDATE_WIFI_ONLY = "auto_update_wifi_only"
        private const val KEY_AUTO_UPDATE_NOTIFICATION = "auto_update_notification"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_LOG_RETENTION = "log_retention_period"
        private const val KEY_HAPTICS_ENABLED = "haptics_feedback_enabled"
        private const val KEY_CA_INSTALLED = "ca_certificate_installed"
        private const val KEY_CA_DISMISSED = "ca_banner_dismissed"
        private const val KEY_HTTPS_FILTERING = "https_deep_filtering_enabled"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
