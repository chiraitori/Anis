package dev.chiraitori.anis.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.chiraitori.anis.MainActivity
import dev.chiraitori.anis.R
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.vpn.VpnController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrustedNetworkManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = evaluate()
        override fun onLost(network: Network) = evaluate()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = evaluate()
    }

    fun start() {
        if (isRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            cm.registerNetworkCallback(request, networkCallback)
            isRegistered = true
            evaluate()
        } catch (_: Exception) {
        }

        scope.launch {
            combine(
                settingsRepository.pauseOnTrustedEnabledFlow,
                settingsRepository.trustedSsidsFlow
            ) { enabled, ssids -> enabled to ssids }
                .distinctUntilChanged()
                .collect { evaluate() }
        }
    }

    private fun evaluate() {
        scope.launch {
            lock.withLock {
                try {
                    val featureEnabled = settingsRepository.pauseOnTrustedEnabledFlow.value
                    val trustedSsids = settingsRepository.trustedSsidsFlow.value
                    val isPausedByUs = settingsRepository.isPausedByTrustedFlow.value
                    val isVpnRunning = dev.chiraitori.anis.vpn.VpnState.isRunningFlow.value

                    if (!featureEnabled || trustedSsids.isEmpty()) {
                        if (isPausedByUs && !isVpnRunning) {
                            settingsRepository.setPausedByTrusted(false)
                            cancelNotification()
                            VpnController.getInstance(context).startVpn()
                        }
                        return@withLock
                    }

                    val currentSsid = getCurrentSsid(context)
                    val onTrusted = currentSsid != null && currentSsid in trustedSsids

                    when {
                        onTrusted && isVpnRunning -> {
                            settingsRepository.setPausedByTrusted(true)
                            VpnController.getInstance(context).stopVpn()
                            showPausedNotification(currentSsid ?: "")
                        }
                        !onTrusted && isPausedByUs && !isVpnRunning -> {
                            settingsRepository.setPausedByTrusted(false)
                            cancelNotification()
                            VpnController.getInstance(context).startVpn()
                        }
                        isVpnRunning && isPausedByUs -> {
                            settingsRepository.setPausedByTrusted(false)
                            cancelNotification()
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun showPausedNotification(ssid: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Trusted Networks",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications when DNS protection is paused on trusted Wi-Fi"
                }
                nm.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val text = if (ssid.isNotBlank()) "Paused on trusted Wi-Fi: $ssid" else "Paused on trusted Wi-Fi network"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("Anis Guard Paused")
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }

    private fun cancelNotification() {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val CHANNEL_ID = "anis_trusted_networks_channel"
        private const val NOTIFICATION_ID = 4040

        fun getCurrentSsid(context: Context): String? {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            return try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
                @Suppress("DEPRECATION")
                val raw = wifiManager.connectionInfo?.ssid ?: return null
                val ssid = raw.trim('"')
                if (ssid.isEmpty() || ssid == "<unknown ssid>") null else ssid
            } catch (_: Exception) {
                null
            }
        }
    }
}
