package dev.chiraitori.anis.vpn.root

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.chiraitori.anis.MainActivity
import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.QueryLogRepository
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.data.model.DnsProtocol
import dev.chiraitori.anis.data.model.QueryStatus
import dev.chiraitori.anis.vpn.DnsDecision
import dev.chiraitori.anis.vpn.DnsEngine
import dev.chiraitori.anis.vpn.DnsPacket
import dev.chiraitori.anis.vpn.DohClient
import dev.chiraitori.anis.vpn.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class RootProxyService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var udpServerJob: Job? = null
    private var tcpServerJob: Job? = null

    private var udpSocket: DatagramSocket? = null
    private var tcpServerSocket: ServerSocket? = null

    private lateinit var blockListRepository: BlockListRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var queryLogRepository: QueryLogRepository
    private lateinit var dnsEngine: DnsEngine
    private lateinit var dohClient: DohClient

    override fun onCreate() {
        super.onCreate()
        blockListRepository = BlockListRepository.getInstance(applicationContext)
        settingsRepository = SettingsRepository.getInstance(applicationContext)
        queryLogRepository = QueryLogRepository.instance
        dnsEngine = DnsEngine(blockListRepository, settingsRepository)
        dohClient = DohClient()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopProxy()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        startProxy()

        return START_STICKY
    }

    private fun startProxy() {
        if (VpnState.isRunningFlow.value) return

        try {
            // Apply iptables redirection rules
            val success = RootIptablesManager.setupRules(
                context = this,
                localPort = RootIptablesManager.DEFAULT_DNS_PORT
            )

            if (!success) {
                Log.e(TAG, "Failed applying root iptables rules")
                stopSelf()
                return
            }

            // Start UDP Listener on 127.0.0.1:5354
            udpSocket = DatagramSocket(
                RootIptablesManager.DEFAULT_DNS_PORT,
                InetAddress.getByName("127.0.0.1")
            )

            // Start TCP Listener on 127.0.0.1:5354
            tcpServerSocket = ServerSocket(
                RootIptablesManager.DEFAULT_DNS_PORT,
                50,
                InetAddress.getByName("127.0.0.1")
            )

            VpnState.setRunning(true)
            startUdpServer()
            startTcpServer()
            Log.i(TAG, "Root DNS Proxy server running on 127.0.0.1:5354 (No VPN slot needed!)")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting RootProxyService", e)
            stopProxy()
        }
    }

    private fun startUdpServer() {
        udpServerJob = serviceScope.launch {
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)

            val socket = udpSocket ?: return@launch

            while (isActive && !socket.isClosed) {
                try {
                    socket.receive(packet)

                    val queryBytes = ByteArray(packet.length)
                    System.arraycopy(packet.data, packet.offset, queryBytes, 0, packet.length)

                    val clientAddress = packet.address
                    val clientPort = packet.port

                    val dnsPacket = DnsPacket.parse(queryBytes)
                    if (dnsPacket != null && !dnsPacket.isResponse) {
                        handleDnsQuery(
                            dnsPacket = dnsPacket,
                            onResponseReady = { responseBytes ->
                                try {
                                    val outPacket = DatagramPacket(
                                        responseBytes,
                                        responseBytes.size,
                                        clientAddress,
                                        clientPort
                                    )
                                    socket.send(outPacket)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to send UDP DNS response", e)
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    if (isActive && !socket.isClosed) {
                        Log.e(TAG, "Error in UDP DNS listener", e)
                    }
                }
            }
        }
    }

    private fun startTcpServer() {
        tcpServerJob = serviceScope.launch {
            val server = tcpServerSocket ?: return@launch

            while (isActive && !server.isClosed) {
                try {
                    val clientSocket = server.accept()
                    serviceScope.launch {
                        handleTcpClient(clientSocket)
                    }
                } catch (e: Exception) {
                    if (isActive && !server.isClosed) {
                        Log.e(TAG, "Error in TCP DNS listener", e)
                    }
                }
            }
        }
    }

    private suspend fun handleTcpClient(socket: Socket) {
        socket.use { client ->
            try {
                val input = client.getInputStream()
                val output = client.getOutputStream()

                // TCP DNS query has a 2-byte prefix indicating length
                val lenHigh = input.read()
                val lenLow = input.read()
                if (lenHigh == -1 || lenLow == -1) return

                val length = (lenHigh shl 8) or lenLow
                val queryBytes = ByteArray(length)
                var bytesRead = 0
                while (bytesRead < length) {
                    val count = input.read(queryBytes, bytesRead, length - bytesRead)
                    if (count == -1) break
                    bytesRead += count
                }

                val dnsPacket = DnsPacket.parse(queryBytes) ?: return
                handleDnsQuery(
                    dnsPacket = dnsPacket,
                    onResponseReady = { responseBytes ->
                        try {
                            output.write((responseBytes.size ushr 8) and 0xFF)
                            output.write(responseBytes.size and 0xFF)
                            output.write(responseBytes)
                            output.flush()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send TCP DNS response", e)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling TCP client", e)
            }
        }
    }

    private fun handleDnsQuery(
        dnsPacket: DnsPacket,
        onResponseReady: (ByteArray) -> Unit
    ) {
        val domain = dnsPacket.qname
        val decision = dnsEngine.evaluate(domain)

        when (decision) {
            is DnsDecision.Block -> {
                val blockBytes = DnsPacket.buildBlockResponse(dnsPacket)
                onResponseReady(blockBytes)

                queryLogRepository.logQuery(
                    domain = domain,
                    queryType = DnsPacket.getTypeName(dnsPacket.qtype),
                    status = QueryStatus.BLOCKED_AD,
                    blockReason = decision.reason,
                    latencyMs = 1L
                )
            }

            is DnsDecision.Rewrite -> {
                val rewriteBytes = DnsPacket.buildIpResponse(dnsPacket, decision.ipAddress)
                onResponseReady(rewriteBytes)

                queryLogRepository.logQuery(
                    domain = domain,
                    queryType = DnsPacket.getTypeName(dnsPacket.qtype),
                    status = decision.status,
                    blockReason = "${decision.reason} (${decision.ipAddress})",
                    latencyMs = 1L
                )
            }

            is DnsDecision.Allow -> {
                serviceScope.launch {
                    val upstream = settingsRepository.upstreamDnsFlow.value
                    val protocol = settingsRepository.dnsProtocolFlow.value
                    val startTime = System.currentTimeMillis()

                    if (protocol == DnsProtocol.DOH && !upstream.dohUrl.isNullOrEmpty()) {
                        val response = dohClient.resolve(upstream.dohUrl, dnsPacket.rawBytes)
                        if (response != null) {
                            val latency = System.currentTimeMillis() - startTime
                            onResponseReady(response)

                            queryLogRepository.logQuery(
                                domain = domain,
                                queryType = DnsPacket.getTypeName(dnsPacket.qtype),
                                status = decision.status,
                                blockReason = null,
                                latencyMs = latency
                            )
                            return@launch
                        }
                    }

                    // Fallback to plain UDP upstream
                    try {
                        val upstreamIp = InetAddress.getByName(upstream.primaryIp)
                        val outPacket = DatagramPacket(
                            dnsPacket.rawBytes,
                            dnsPacket.rawBytes.size,
                            upstreamIp,
                            53
                        )

                        DatagramSocket().use { forwardSock ->
                            forwardSock.soTimeout = 4000
                            forwardSock.send(outPacket)

                            val recvBuffer = ByteArray(4096)
                            val inPacket = DatagramPacket(recvBuffer, recvBuffer.size)
                            forwardSock.receive(inPacket)

                            val latency = System.currentTimeMillis() - startTime
                            val resBytes = ByteArray(inPacket.length)
                            System.arraycopy(inPacket.data, inPacket.offset, resBytes, 0, inPacket.length)

                            onResponseReady(resBytes)

                            queryLogRepository.logQuery(
                                domain = domain,
                                queryType = DnsPacket.getTypeName(dnsPacket.qtype),
                                status = decision.status,
                                blockReason = null,
                                latencyMs = latency
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Upstream DNS resolution failed", e)
                    }
                }
            }
        }
    }

    private fun stopProxy() {
        udpServerJob?.cancel()
        udpServerJob = null

        tcpServerJob?.cancel()
        tcpServerJob = null

        try {
            udpSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        udpSocket = null

        try {
            tcpServerSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        tcpServerSocket = null

        // Teardown iptables rules (vital for retaining internet connectivity)
        RootIptablesManager.teardownRules()

        VpnState.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopProxy()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        RootIptablesManager.teardownRules()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anis Root Mode Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Anis is actively protecting via Root iptables"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, RootProxyService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Anis Root Shield Active (No VPN)")
            .setContentText("Transparent iptables DNS interception & adblocking active")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val TAG = "RootProxyService"
        const val CHANNEL_ID = "anis_root_proxy_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "dev.chiraitori.anis.action.ROOT_START"
        const val ACTION_STOP = "dev.chiraitori.anis.action.ROOT_STOP"

        fun start(context: Context) {
            val intent = Intent(context, RootProxyService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RootProxyService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
