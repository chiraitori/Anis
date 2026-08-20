package dev.chiraitori.anis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.chiraitori.anis.AnisApplication
import dev.chiraitori.anis.data.model.AppLanguage
import dev.chiraitori.anis.data.model.AutoUpdateFrequency
import dev.chiraitori.anis.data.model.BlockListSource
import dev.chiraitori.anis.data.model.CustomDnsRule
import dev.chiraitori.anis.data.model.DnsProtocol
import dev.chiraitori.anis.data.model.DnsResponseType
import dev.chiraitori.anis.data.model.LogRetention
import dev.chiraitori.anis.data.model.ProtectionMode
import dev.chiraitori.anis.data.model.ProtectionProfile
import dev.chiraitori.anis.data.model.RuleCategory
import dev.chiraitori.anis.data.model.ThemeMode
import dev.chiraitori.anis.data.model.UpstreamDnsProvider
import dev.chiraitori.anis.service.TrustedNetworkManager
import dev.chiraitori.anis.vpn.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AnisApplication
    private val blockListRepo = app.blockListRepository
    private val profileManager = app.profileManager
    private val settingsRepo = app.settingsRepository
    private val firewallRepo = app.firewallRepository
    private val queryLogRepo = app.queryLogRepository
    private val vpnController = app.vpnController

    val isVpnRunning = VpnState.isRunningFlow
    val isStarting = VpnState.isStartingFlow
    val stats = queryLogRepo.statsFlow
    val blockLists = blockListRepo.sourcesFlow
    val isUpdatingLists = blockListRepo.isUpdatingFlow
    val updateProgress = blockListRepo.updateProgressFlow
    val firewallApps = firewallRepo.installedAppsFlow
    val isFirewallLoading = firewallRepo.isLoadingFlow
    val queryLogs = queryLogRepo.logsFlow
    val topBlockedDomains = queryLogRepo.topBlockedDomainsFlow
    val topApps = queryLogRepo.topAppsFlow

    val upstreamDns = settingsRepo.upstreamDnsFlow
    val dnsProtocol = settingsRepo.dnsProtocolFlow
    val protectionMode = settingsRepo.protectionModeFlow
    val dnsResponseType = settingsRepo.dnsResponseTypeFlow
    val autoUpdateFrequency = settingsRepo.autoUpdateFrequencyFlow
    val autoUpdateWifiOnly = settingsRepo.autoUpdateWifiOnlyFlow
    val autoUpdateNotification = settingsRepo.autoUpdateNotificationFlow
    val autoReconnect = settingsRepo.autoReconnectFlow
    val themeMode = settingsRepo.themeModeFlow
    val appLanguage = settingsRepo.appLanguageFlow
    val logRetention = settingsRepo.logRetentionFlow
    val hapticsEnabled = settingsRepo.hapticsEnabledFlow
    val startOnBoot = settingsRepo.startOnBootFlow

    val whitelist = settingsRepo.whitelistFlow
    val blacklist = settingsRepo.blacklistFlow
    val whitelistedApps = settingsRepo.whitelistedAppsFlow
    val customRules = settingsRepo.customRulesFlow
    val trustedSsids = settingsRepo.trustedSsidsFlow
    val pauseOnTrusted = settingsRepo.pauseOnTrustedEnabledFlow
    val isPausedByTrusted = settingsRepo.isPausedByTrustedFlow

    val activeProfile = profileManager.activeProfileFlow
    val profiles = profileManager.profilesFlow

    val caManager = dev.chiraitori.anis.vpn.CertificateAuthorityManager.getInstance(application)

    private val _isRootAvailableFlow = MutableStateFlow(false)
    val isRootAvailableFlow = _isRootAvailableFlow.asStateFlow()

    private val _isOnboardingCompletedFlow = MutableStateFlow(settingsRepo.isOnboardingCompleted)
    val isOnboardingCompletedFlow = _isOnboardingCompletedFlow.asStateFlow()

    val isCaInstalledFlow = settingsRepo.isCaInstalledFlow
    val isCaDismissedFlow = settingsRepo.isCaDismissedFlow
    val httpsFilteringEnabledFlow = settingsRepo.httpsFilteringEnabledFlow

    fun setHttpsFilteringEnabled(enabled: Boolean) {
        settingsRepo.httpsFilteringEnabled = enabled
        vpnController.restartProtection()
    }

    private val _safeSearchFlow = MutableStateFlow(settingsRepo.safeSearchEnabled)
    val safeSearchFlow = _safeSearchFlow.asStateFlow()

    private val _youtubeRestrictedFlow = MutableStateFlow(settingsRepo.youtubeRestrictedMode)
    val youtubeRestrictedFlow = _youtubeRestrictedFlow.asStateFlow()

    init {
        loadFirewallApps()
        checkRootStatus()
        checkIsCaInstalled()
    }

    fun checkIsCaInstalled(): Boolean {
        val inTrustStore = caManager.isCaInstalledInTrustStore()
        val magiskDir = java.io.File("/data/adb/modules/anis_root_ca")
        val isInstalled = inTrustStore || magiskDir.exists()
        settingsRepo.isCaInstalled = isInstalled
        return isInstalled
    }

    fun dismissCaWarning() {
        settingsRepo.isCaDismissed = true
    }

    fun markCaInstalled() {
        checkIsCaInstalled()
    }

    fun checkRootStatus() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRootAvailableFlow.value = dev.chiraitori.anis.vpn.root.RootUtils.isRootAvailable()
        }
    }

    fun setProtectionMode(mode: ProtectionMode) {
        settingsRepo.setProtectionMode(mode)
        vpnController.restartProtection()
    }

    fun installSystemCaCert(): Boolean {
        val pem = caManager.getOrCreateCaCertificatePem()
        val success = dev.chiraitori.anis.vpn.root.RootIptablesManager.installCaCertificateToSystem(pem)
        if (success) {
            settingsRepo.isCaInstalled = true
        }
        return success
    }

    fun completeOnboarding() {
        settingsRepo.isOnboardingCompleted = true
        _isOnboardingCompletedFlow.value = true
    }

    fun isVpnPrepared(): Boolean {
        return vpnController.isVpnPrepared()
    }

    fun toggleVpn() {
        if (isVpnRunning.value) {
            vpnController.stopVpn()
        } else {
            vpnController.startVpn()
        }
    }

    fun startVpn() {
        vpnController.startVpn()
    }

    fun stopVpn() {
        vpnController.stopVpn()
    }

    // Profile operations
    fun switchToProfile(profileId: String) {
        profileManager.switchToProfile(profileId)
        _safeSearchFlow.value = settingsRepo.safeSearchEnabled
        _youtubeRestrictedFlow.value = settingsRepo.youtubeRestrictedMode
    }

    fun updateProfile(profile: ProtectionProfile) {
        profileManager.updateProfile(profile)
    }

    // Blocklist operations
    fun toggleBlockList(id: String, enabled: Boolean) {
        blockListRepo.toggleList(id, enabled)
    }

    fun enableAllBlockLists() {
        blockListRepo.enableAllBlockLists()
    }

    fun disableAllBlockLists() {
        blockListRepo.disableAllBlockLists()
    }

    fun addCustomBlockList(name: String, url: String, category: RuleCategory = RuleCategory.CUSTOM) {
        blockListRepo.addCustomList(name, url, category)
    }

    fun removeCustomBlockList(id: String) {
        blockListRepo.removeCustomList(id)
    }

    fun updateAllBlockLists() {
        viewModelScope.launch {
            blockListRepo.updateAllLists()
        }
    }

    fun updateBlockList(id: String) {
        viewModelScope.launch {
            blockListRepo.updateList(id)
        }
    }

    // Firewall operations
    fun loadFirewallApps() {
        viewModelScope.launch {
            firewallRepo.loadInstalledApps()
        }
    }

    fun toggleAppFirewall(packageName: String, blocked: Boolean) {
        firewallRepo.toggleAppBlock(packageName, blocked)
    }

    fun blockAllFirewallApps(userAppsOnly: Boolean = true) {
        firewallRepo.blockAll(userAppsOnly)
    }

    fun unblockAllFirewallApps() {
        firewallRepo.unblockAll()
    }

    fun toggleAppBypassVpn(packageName: String, isBypassed: Boolean) {
        settingsRepo.toggleAppBypassVpn(packageName, isBypassed)
        vpnController.restartVpn()
    }

    // Trusted Wi-Fi operations
    fun addTrustedSsid(ssid: String) {
        settingsRepo.addTrustedSsid(ssid)
    }

    fun removeTrustedSsid(ssid: String) {
        settingsRepo.removeTrustedSsid(ssid)
    }

    fun togglePauseOnTrusted(enabled: Boolean) {
        settingsRepo.setPauseOnTrustedEnabled(enabled)
    }

    fun getCurrentConnectedSsid(): String? {
        return TrustedNetworkManager.getCurrentSsid(getApplication())
    }

    // Settings operations
    fun setUpstreamDns(provider: UpstreamDnsProvider) {
        settingsRepo.setUpstreamDns(provider)
        vpnController.restartVpn()
    }

    fun setCustomUpstreamDns(name: String, primaryIp: String, secondaryIp: String = "", dohUrl: String? = null) {
        settingsRepo.setCustomUpstreamDns(name, primaryIp, secondaryIp, dohUrl)
        vpnController.restartVpn()
    }

    fun setDnsProtocol(protocol: DnsProtocol) {
        settingsRepo.setDnsProtocol(protocol)
        vpnController.restartVpn()
    }

    fun setSafeSearchEnabled(enabled: Boolean) {
        settingsRepo.safeSearchEnabled = enabled
        _safeSearchFlow.value = enabled
        vpnController.restartVpn()
    }

    fun setYoutubeRestrictedMode(enabled: Boolean) {
        settingsRepo.youtubeRestrictedMode = enabled
        _youtubeRestrictedFlow.value = enabled
        vpnController.restartVpn()
    }

    fun setPauseOnTrustedEnabled(enabled: Boolean) {
        settingsRepo.setPauseOnTrustedEnabled(enabled)
    }

    fun whitelistApp(packageName: String) {
        toggleAppBypassVpn(packageName, true)
    }

    fun unwhitelistApp(packageName: String) {
        toggleAppBypassVpn(packageName, false)
    }

    fun toggleSafeSearch(enabled: Boolean) {
        setSafeSearchEnabled(enabled)
    }

    fun toggleYouTubeRestricted(enabled: Boolean) {
        setYoutubeRestrictedMode(enabled)
    }

    fun addCustomRule(domain: String, targetIp: String) {
        settingsRepo.addCustomRule(domain, targetIp)
    }

    fun removeCustomRule(id: String) {
        settingsRepo.removeCustomRule(id)
    }

    fun addWhitelistDomain(domain: String) {
        settingsRepo.addWhitelistDomain(domain)
    }

    fun removeWhitelistDomain(domain: String) {
        settingsRepo.removeWhitelistDomain(domain)
    }

    fun addBlacklistDomain(domain: String) {
        settingsRepo.addBlacklistDomain(domain)
    }

    fun removeBlacklistDomain(domain: String) {
        settingsRepo.removeBlacklistDomain(domain)
    }

    fun setStartOnBoot(enabled: Boolean) {
        settingsRepo.startOnBoot = enabled
    }

    fun setDnsResponseType(type: DnsResponseType) {
        settingsRepo.setDnsResponseType(type)
        vpnController.restartVpn()
    }

    fun setAutoUpdateFrequency(frequency: AutoUpdateFrequency) {
        settingsRepo.setAutoUpdateFrequency(frequency)
        dev.chiraitori.anis.service.BlockListUpdateScheduler.schedule(
            getApplication(),
            frequency,
            settingsRepo.autoUpdateWifiOnlyFlow.value
        )
    }

    fun setAutoUpdateWifiOnly(wifiOnly: Boolean) {
        settingsRepo.setAutoUpdateWifiOnly(wifiOnly)
        dev.chiraitori.anis.service.BlockListUpdateScheduler.schedule(
            getApplication(),
            settingsRepo.autoUpdateFrequencyFlow.value,
            wifiOnly
        )
    }

    fun setAutoUpdateNotification(notify: Boolean) {
        settingsRepo.setAutoUpdateNotification(notify)
    }

    fun setAutoReconnect(reconnect: Boolean) {
        settingsRepo.setAutoReconnect(reconnect)
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepo.setThemeMode(mode)
    }

    fun setAppLanguage(language: AppLanguage) {
        settingsRepo.setAppLanguage(language)
    }

    fun setLogRetention(retention: LogRetention) {
        settingsRepo.setLogRetention(retention)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        settingsRepo.setHapticsEnabled(enabled)
    }

    fun clearAllLogs() {
        queryLogRepo.clearLogs()
        queryLogRepo.resetStats()
    }

    fun resetAllStats() {
        queryLogRepo.resetStats()
    }

    fun exportBackup(): String {
        return org.json.JSONObject(settingsRepo.exportBackupJson())
            .put("blockLists", blockListRepo.exportBackupJson())
            .toString(2)
    }

    fun importBackup(jsonString: String): Boolean {
        val root = runCatching { org.json.JSONObject(jsonString) }.getOrNull() ?: return false
        val settingsSuccess = settingsRepo.importBackupJson(jsonString)
        val listsSuccess = root.optJSONArray("blockLists")?.let(blockListRepo::importBackupJson) ?: true
        val success = settingsSuccess && listsSuccess
        if (success) {
            _safeSearchFlow.value = settingsRepo.safeSearchEnabled
            _youtubeRestrictedFlow.value = settingsRepo.youtubeRestrictedMode
            dev.chiraitori.anis.service.BlockListUpdateScheduler.schedule(
                getApplication(),
                settingsRepo.autoUpdateFrequencyFlow.value,
                settingsRepo.autoUpdateWifiOnlyFlow.value
            )
            vpnController.restartVpn()
            loadFirewallApps()
        }
        return success
    }

    fun clearLogs() {
        queryLogRepo.clearLogs()
    }

    fun resetStats() {
        queryLogRepo.resetStats()
    }
}
