package dev.chiraitori.anis.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.chiraitori.anis.data.model.AppFirewallItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FirewallRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val _installedAppsFlow = MutableStateFlow<List<AppFirewallItem>>(emptyList())
    val installedAppsFlow: StateFlow<List<AppFirewallItem>> = _installedAppsFlow.asStateFlow()

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        _isLoadingFlow.value = true
        val pm = context.packageManager
        val blockedSet = settingsRepository.firewallBlockedAppsFlow.value

        val apps = try {
            val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val selfPackage = context.packageName

            installedPackages
                .filter { it.packageName != selfPackage }
                .map { appInfo ->
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val appName = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        appInfo.packageName
                    }
                    AppFirewallItem(
                        packageName = appInfo.packageName,
                        appName = appName,
                        isBlocked = appInfo.packageName in blockedSet,
                        isSystemApp = isSystem,
                        uid = appInfo.uid
                    )
                }
                .sortedWith(compareBy({ !it.isBlocked }, { it.isSystemApp }, { it.appName.lowercase() }))
        } catch (e: Exception) {
            emptyList()
        }

        _installedAppsFlow.value = apps
        _isLoadingFlow.value = false
    }

    fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        settingsRepository.setAppBlockedInFirewall(packageName, isBlocked)
        _installedAppsFlow.value = _installedAppsFlow.value.map {
            if (it.packageName == packageName) it.copy(isBlocked = isBlocked) else it
        }
    }

    fun blockAll(userAppsOnly: Boolean = true) {
        val targets = _installedAppsFlow.value
            .filter { if (userAppsOnly) !it.isSystemApp else true }
            .map { it.packageName }
            .toSet()

        settingsRepository.setAllAppsBlockedInFirewall(targets, true)
        _installedAppsFlow.value = _installedAppsFlow.value.map {
            if (it.packageName in targets) it.copy(isBlocked = true) else it
        }
    }

    fun unblockAll() {
        val allPkgs = _installedAppsFlow.value.map { it.packageName }.toSet()
        settingsRepository.setAllAppsBlockedInFirewall(allPkgs, false)
        _installedAppsFlow.value = _installedAppsFlow.value.map {
            it.copy(isBlocked = false)
        }
    }

    companion object {
        @Volatile
        private var instance: FirewallRepository? = null

        fun getInstance(context: Context): FirewallRepository {
            return instance ?: synchronized(this) {
                val settings = SettingsRepository.getInstance(context)
                instance ?: FirewallRepository(context.applicationContext, settings).also { instance = it }
            }
        }
    }
}
