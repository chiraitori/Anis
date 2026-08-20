package dev.chiraitori.anis.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.chiraitori.anis.MainActivity
import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.QueryLogRepository
import dev.chiraitori.anis.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var goTunnelAdapter: GoTunnelAdapter
    private lateinit var connectivityManager: ConnectivityManager
    private var underlyingNetwork: Network? = null
    private var underlyingNetworkWasLost = false
    private var reconnectJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val shouldReconnect = underlyingNetworkWasLost ||
                (underlyingNetwork != null && underlyingNetwork != network)
            underlyingNetwork = network
            underlyingNetworkWasLost = false
            if (shouldReconnect) scheduleNetworkRecovery()
        }

        override fun onLost(network: Network) {
            if (network == underlyingNetwork) {
                underlyingNetwork = null
                underlyingNetworkWasLost = true
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val blockListRepository = BlockListRepository.getInstance(applicationContext)
        settingsRepository = SettingsRepository.getInstance(applicationContext)
        goTunnelAdapter = GoTunnelAdapter(
            context = applicationContext,
            blockListRepository = blockListRepository,
            settingsRepository = settingsRepository,
            queryLogRepository = QueryLogRepository.instance,
            scope = serviceScope,
            protectSocket = { fd ->
                try {
                    protect(fd)
                } catch (_: Exception) {
                    false
                }
            }
        )
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build(),
            networkCallback
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Active - Go tunnel protection"))
        startVpn()
        return if (settingsRepository.autoReconnectFlow.value) START_STICKY else START_NOT_STICKY
    }

    private fun scheduleNetworkRecovery() {
        if (!settingsRepository.autoReconnectFlow.value || vpnInterface == null) return
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            delay(700)
            Log.i(TAG, "Underlying network changed; rebuilding VPN tunnel")
            goTunnelAdapter.stop()
            runCatching { vpnInterface?.close() }
            vpnInterface = null
            startVpn()
        }
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        val enableHttps = settingsRepository.httpsFilteringEnabledFlow.value &&
            settingsRepository.isCaInstalledFlow.value

        try {
            val builder = Builder()
                .setSession("Anis")
                .setBlocking(true)
                .setMtu(1500)
                .addAddress(LOCAL_IPV4, 32)
                .addDnsServer(DNS_IPV4)
                .addRoute(DNS_IPV4, 32)
                .addAddress(LOCAL_IPV6, 128)
                .addDnsServer(DNS_IPV6)
                .addRoute(DNS_IPV6, 128)

            // HTTPS filtering needs the userspace stack to receive TCP traffic.
            // DNS-only mode captures only the two synthetic DNS endpoints.
            if (enableHttps) {
                builder.addRoute("0.0.0.0", 0)
            } else {
                builder.allowBypass()
            }

            try {
                builder.addDisallowedApplication(packageName)
            } catch (error: Exception) {
                Log.w(TAG, "Could not exclude Anis from its own tunnel", error)
            }

            settingsRepository.whitelistedAppsFlow.value.forEach { packageName ->
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (error: Exception) {
                    Log.w(TAG, "Could not bypass $packageName", error)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setUnderlyingNetworks(null)
                builder.setMetered(false)
            }

            val established = builder.establish()
            if (established == null) {
                Log.e(TAG, "Android refused to establish the VPN interface")
                VpnState.setStarting(false)
                stopSelf()
                return
            }

            vpnInterface = established
            goTunnelAdapter.start(established, enableHttps)
            VpnState.setRunning(true)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to start the Go VPN tunnel", error)
            stopVpn()
        }
    }

    private fun stopVpn(stopService: Boolean = true) {
        goTunnelAdapter.stop()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
            // Already closed by Android or the native engine.
        }
        vpnInterface = null
        VpnState.setRunning(false)
        VpnState.setStarting(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (stopService) stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        reconnectJob?.cancel()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        stopVpn(stopService = false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anis Network Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active DNS, HTTPS, and firewall protection"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AdBlockVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Anis DNS & Firewall Guard")
            .setContentText(contentText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "AdBlockVpnService"
        private const val LOCAL_IPV4 = "10.0.0.2"
        private const val DNS_IPV4 = "10.0.0.1"
        private const val LOCAL_IPV6 = "fd00::2"
        private const val DNS_IPV6 = "fd00::1"

        const val CHANNEL_ID = "anis_vpn_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "dev.chiraitori.anis.action.START"
        const val ACTION_STOP = "dev.chiraitori.anis.action.STOP"
    }
}
