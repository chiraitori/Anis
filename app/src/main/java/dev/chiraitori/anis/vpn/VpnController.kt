package dev.chiraitori.anis.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
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
        _isStartingFlow.value = false
    }

    fun setStarting(starting: Boolean) {
        _isStartingFlow.value = starting
        if (starting) {
            _isRunningFlow.value = false
        }
    }

    val isActiveOrStarting: Boolean
        get() = _isRunningFlow.value || _isStartingFlow.value
}

class VpnController(private val context: Context) {

    private val settingsRepo by lazy { SettingsRepository.getInstance(context) }
    private var activeMode: ProtectionMode? = null

    fun isVpnPrepared(): Boolean {
        return VpnService.prepare(context) == null
    }

    @Synchronized
    fun startProtection() {
        if (VpnState.isActiveOrStarting) return

        val mode = settingsRepo.protectionModeFlow.value
        if (mode == ProtectionMode.LOCAL_VPN && !isVpnPrepared()) {
            VpnState.setRunning(false)
            return
        }

        activeMode = mode
        VpnState.setStarting(true)

        try {
            if (mode == ProtectionMode.ROOT_PROXY) {
                RootProxyService.start(context)
            } else {
                val intent = Intent(context, AdBlockVpnService::class.java).apply {
                    action = AdBlockVpnService.ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to dispatch protection start", error)
            activeMode = null
            VpnState.setRunning(false)
        }
    }

    @Synchronized
    fun stopProtection() {
        if (!VpnState.isActiveOrStarting && activeMode == null) {
            VpnState.setRunning(false)
            return
        }

        // A VpnService stays bound by Android while its TUN is active, so stopService()
        // alone does not destroy it. Deliver an explicit stop command so the service
        // closes the native tunnel and file descriptor before calling stopSelf().
        VpnState.setStarting(false)
        val modeToStop = activeMode ?: settingsRepo.protectionModeFlow.value
        try {
            when (modeToStop) {
                ProtectionMode.LOCAL_VPN -> context.startService(
                    Intent(context, AdBlockVpnService::class.java).apply {
                        action = AdBlockVpnService.ACTION_STOP
                    }
                )
                ProtectionMode.ROOT_PROXY -> RootProxyService.stop(context)
            }
            activeMode = null
            VpnState.setRunning(false)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to dispatch protection stop", error)
            when (modeToStop) {
                ProtectionMode.LOCAL_VPN -> context.stopService(
                    Intent(context, AdBlockVpnService::class.java)
                )
                ProtectionMode.ROOT_PROXY -> context.stopService(
                    Intent(context, RootProxyService::class.java)
                )
            }
            activeMode = null
            VpnState.setRunning(false)
        }
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
        private const val TAG = "VpnController"

        @Volatile
        private var instance: VpnController? = null

        fun getInstance(context: Context): VpnController {
            return instance ?: synchronized(this) {
                instance ?: VpnController(context.applicationContext).also { instance = it }
            }
        }
    }
}
