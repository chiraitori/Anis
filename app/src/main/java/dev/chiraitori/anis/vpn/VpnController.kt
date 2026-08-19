package dev.chiraitori.anis.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.data.model.ProtectionMode
import dev.chiraitori.anis.vpn.root.RootProxyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnState {
    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

    private val _isStartingFlow = MutableStateFlow(false)
    val isStartingFlow: StateFlow<Boolean> = _isStartingFlow.asStateFlow()

    fun setRunning(running: Boolean) {
        _isRunningFlow.value = running
        if (running) {
            _isStartingFlow.value = false
        }
    }

    fun setStarting(starting: Boolean) {
        _isStartingFlow.value = starting
    }
}

class VpnController(private val context: Context) {

    private val settingsRepo by lazy { SettingsRepository.getInstance(context) }

    fun isVpnPrepared(): Boolean {
        return VpnService.prepare(context) == null
    }

    fun startProtection() {
        VpnState.setStarting(true)
        val mode = settingsRepo.protectionModeFlow.value

        if (mode == ProtectionMode.ROOT_PROXY) {
            RootProxyService.start(context)
        } else {
            val intent = Intent(context, AdBlockVpnService::class.java).apply {
                action = AdBlockVpnService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopProtection() {
        // Stop both to ensure clean teardown
        val vpnIntent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_STOP
        }
        context.startService(vpnIntent)
        RootProxyService.stop(context)

        VpnState.setRunning(false)
    }

    fun restartProtection() {
        if (VpnState.isRunningFlow.value) {
            stopProtection()
            startProtection()
        }
    }

    // Aliases for compatibility
    fun startVpn() = startProtection()
    fun stopVpn() = stopProtection()
    fun restartVpn() = restartProtection()

    companion object {
        @Volatile
        private var instance: VpnController? = null

        fun getInstance(context: Context): VpnController {
            return instance ?: synchronized(this) {
                instance ?: VpnController(context.applicationContext).also { instance = it }
            }
        }
    }
}
