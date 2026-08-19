package dev.chiraitori.anis.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.chiraitori.anis.MainActivity
import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.QueryLogRepository
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.data.model.DnsProtocol
import dev.chiraitori.anis.data.model.QueryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private lateinit var blockListRepository: BlockListRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var queryLogRepository: QueryLogRepository
    private lateinit var dnsEngine: DnsEngine
    private lateinit var dohClient: DohClient

    // Upstream DNS forwarding socket
    private var forwardSocket: DatagramSocket? = null

    // Cache of recent transaction IDs to original request endpoints
    private data class RequestEndpoint(val clientIp: InetAddress, val clientPort: Int, val domain: String, val startTime: Long)
    private val pendingRequests = ConcurrentHashMap<Short, RequestEndpoint>()

    override fun onCreate() {
        super.onCreate()
        blockListRepository = BlockListRepository.getInstance(applicationContext)
        settingsRepository = SettingsRepository.getInstance(applicationContext)
        queryLogRepository = QueryLogRepository.instance
        dnsEngine = DnsEngine(blockListRepository, settingsRepository)
        dohClient = DohClient { fd ->
            try {
                protect(fd)
            } catch (e: Exception) {
                false
            }
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Active - Blocking ads & trackers"))
        startVpn()

        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
                .setSession("Anis")
                .setMtu(1500)
                .addAddress("10.233.1.2", 32)
                .addDnsServer("10.233.1.1")
                .addRoute("10.233.1.1", 32)
                // Capture standard DNS endpoints
                .addRoute("1.1.1.1", 32)
                .addRoute("1.0.0.1", 32)
                .addRoute("8.8.8.8", 32)
                .addRoute("8.8.4.4", 32)
                .addRoute("9.9.9.9", 32)
                .addRoute("94.140.14.14", 32)

            // Exclude our own app so network fetching doesn't loopback
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error excluding self package", e)
            }

            // Exclude user whitelisted / bypassed apps
            val whitelistedApps = settingsRepository.whitelistedAppsFlow.value
            for (pkg in whitelistedApps) {
                try {
                    builder.addDisallowedApplication(pkg)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not exclude whitelisted app: $pkg")
                }
            }

            // Allow bypass for split tunneling
            builder.allowBypass()

            // Inherit unmetered network state on Android 10+ (like BlockAds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                stopSelf()
                return
            }

            // Initialize UDP socket for upstream forwarding and protect it
            forwardSocket = DatagramSocket().also { sock ->
                protect(sock)
                sock.soTimeout = 4000
            }

            VpnState.setRunning(true)
            startVpnLoop()
            startUpstreamListenerLoop()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            stopVpn()
        }
    }

    private fun startVpnLoop() {
        vpnJob = serviceScope.launch {
            val vpnFd = vpnInterface?.fileDescriptor ?: return@launch
            val inputStream = FileInputStream(vpnFd)
            val outputStream = FileOutputStream(vpnFd)
            val packetBuffer = ByteBuffer.allocate(32767)

            val upstream = settingsRepository.upstreamDnsFlow.value
            val upstreamIp = try {
                InetAddress.getByName(upstream.primaryIp)
            } catch (e: Exception) {
                InetAddress.getByName("1.1.1.1")
            }
            val protocol = settingsRepository.dnsProtocolFlow.value

            while (isActive && vpnInterface != null) {
                try {
                    packetBuffer.clear()
                    val length = inputStream.read(packetBuffer.array())
                    if (length <= 0) continue

                    packetBuffer.limit(length)
                    val ipPacket = IpPacket.parse(packetBuffer) ?: continue

                    // Check if it's UDP traffic
                    if (ipPacket.protocol == IpPacket.PROTOCOL_UDP) {
                        val udpPayloadBuffer = ByteBuffer.wrap(ipPacket.payload)
                        if (udpPayloadBuffer.remaining() >= 8) {
                            val srcPort = udpPayloadBuffer.short.toInt() and 0xFFFF
                            val dstPort = udpPayloadBuffer.short.toInt() and 0xFFFF
                            val udpLen = udpPayloadBuffer.short.toInt() and 0xFFFF
                            val udpChecksum = udpPayloadBuffer.short

                            val dnsPayload = ByteArray(udpPayloadBuffer.remaining())
                            udpPayloadBuffer.get(dnsPayload)

                            val dnsPacket = DnsPacket.parse(dnsPayload)
                            if (dnsPacket != null && !dnsPacket.isResponse) {
                                handleDnsQuery(
                                    dnsPacket = dnsPacket,
                                    clientIp = ipPacket.sourceIp,
                                    clientPort = srcPort,
                                    dnsServerIp = ipPacket.destinationIp,
                                    dnsServerPort = dstPort,
                                    upstreamIp = upstreamIp,
                                    dohUrl = upstream.dohUrl,
                                    useDoh = protocol == DnsProtocol.DOH && !upstream.dohUrl.isNullOrEmpty(),
                                    outputStream = outputStream
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error in VPN packet read loop", e)
                    }
                }
            }
        }
    }

    private fun handleDnsQuery(
        dnsPacket: DnsPacket,
        clientIp: InetAddress,
        clientPort: Int,
        dnsServerIp: InetAddress,
        dnsServerPort: Int,
        upstreamIp: InetAddress,
        dohUrl: String?,
        useDoh: Boolean,
        outputStream: FileOutputStream
    ) {
        val domain = dnsPacket.qname
        val decision = dnsEngine.evaluate(domain)

        when (decision) {
            is DnsDecision.Block -> {
                // Synthesize local 0.0.0.0 / NXDOMAIN response
                val syntheticDnsBytes = DnsPacket.buildBlockResponse(dnsPacket)
                val responsePacket = IpPacket.buildUdpIpPacket(
                    sourceIp = dnsServerIp,
                    destinationIp = clientIp,
                    sourcePort = dnsServerPort,
                    destinationPort = clientPort,
                    udpPayload = syntheticDnsBytes
                )

                synchronized(outputStream) {
                    outputStream.write(responsePacket)
                    outputStream.flush()
                }

                queryLogRepository.logQuery(
                    domain = domain,
                    queryType = DnsPacket.getTypeName(dnsPacket.qtype),
                    status = QueryStatus.BLOCKED_AD,
                    blockReason = decision.reason,
                    latencyMs = 1L
                )
            }

            is DnsDecision.Rewrite -> {
                // Synthesize custom IP response (SafeSearch, YouTube restriction, or custom DNS rewrite)
                val syntheticDnsBytes = DnsPacket.buildIpResponse(dnsPacket, decision.ipAddress)
                val responsePacket = IpPacket.buildUdpIpPacket(
                    sourceIp = dnsServerIp,
                    destinationIp = clientIp,
                    sourcePort = dnsServerPort,
                    destinationPort = clientPort,
                    udpPayload = syntheticDnsBytes
                )

                synchronized(outputStream) {
                    outputStream.write(responsePacket)
                    outputStream.flush()
                }

                queryLogRepository.logQuery(
                    domain = domain,
                    queryType = DnsPacket.getTypeName(dnsPacket.qtype),
                    status = decision.status,
                    blockReason = "${decision.reason} (${decision.ipAddress})",
                    latencyMs = 1L
                )
            }

            is DnsDecision.Allow -> {
                if (useDoh && dohUrl != null) {
                    // Resolve via DoH (DNS-over-HTTPS)
                    serviceScope.launch {
                        val startTime = System.currentTimeMillis()
                        val responseBytes = dohClient.resolve(dohUrl, dnsPacket.rawBytes)

                        if (responseBytes != null) {
                            val latency = System.currentTimeMillis() - startTime
                            val responseIpPacket = IpPacket.buildUdpIpPacket(
                                sourceIp = dnsServerIp,
                                destinationIp = clientIp,
                                sourcePort = dnsServerPort,
                                destinationPort = clientPort,
                                udpPayload = responseBytes
                            )

                            synchronized(outputStream) {
                                outputStream.write(responseIpPacket)
                                outputStream.flush()
                            }

                            queryLogRepository.logQuery(
                                domain = domain,
                                queryType = DnsPacket.getTypeName(dnsPacket.qtype),
                                status = decision.status,
                                blockReason = null,
                                latencyMs = latency
                            )
                        } else {
                            // Fallback to plain UDP if DoH failed
                            forwardViaUdp(dnsPacket, clientIp, clientPort, upstreamIp)
                        }
                    }
                } else {
                    forwardViaUdp(dnsPacket, clientIp, clientPort, upstreamIp)
                }
            }
        }
    }

    private fun forwardViaUdp(
        dnsPacket: DnsPacket,
        clientIp: InetAddress,
        clientPort: Int,
        upstreamIp: InetAddress
    ) {
        pendingRequests[dnsPacket.transactionId] = RequestEndpoint(
            clientIp = clientIp,
            clientPort = clientPort,
            domain = dnsPacket.qname,
            startTime = System.currentTimeMillis()
        )

        try {
            val outPacket = DatagramPacket(
                dnsPacket.rawBytes,
                dnsPacket.rawBytes.size,
                upstreamIp,
                53
            )
            forwardSocket?.send(outPacket)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send query to upstream UDP DNS", e)
        }
    }

    private fun startUpstreamListenerLoop() {
        serviceScope.launch {
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive && vpnInterface != null) {
                try {
                    val socket = forwardSocket ?: break
                    socket.receive(packet)

                    val responseBytes = ByteArray(packet.length)
                    System.arraycopy(packet.data, packet.offset, responseBytes, 0, packet.length)

                    val dnsResponse = DnsPacket.parse(responseBytes)
                    if (dnsResponse != null) {
                        val endpoint = pendingRequests.remove(dnsResponse.transactionId)
                        if (endpoint != null) {
                            val latency = System.currentTimeMillis() - endpoint.startTime

                            val vpnFd = vpnInterface?.fileDescriptor
                            if (vpnFd != null) {
                                val outputStream = FileOutputStream(vpnFd)
                                val responseIpPacket = IpPacket.buildUdpIpPacket(
                                    sourceIp = InetAddress.getByName("10.233.1.1"),
                                    destinationIp = endpoint.clientIp,
                                    sourcePort = 53,
                                    destinationPort = endpoint.clientPort,
                                    udpPayload = responseBytes
                                )

                                synchronized(outputStream) {
                                    outputStream.write(responseIpPacket)
                                    outputStream.flush()
                                }
                            }

                            queryLogRepository.logQuery(
                                domain = endpoint.domain,
                                queryType = DnsPacket.getTypeName(dnsResponse.qtype),
                                status = QueryStatus.ALLOWED,
                                blockReason = null,
                                latencyMs = latency
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        // socket timeout
                    }
                }
            }
        }
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnJob = null

        try {
            forwardSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        forwardSocket = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null

        VpnState.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anis DNS Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active DNS adblocking and firewall status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, AdBlockVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
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
        const val TAG = "AdBlockVpnService"
        const val CHANNEL_ID = "anis_vpn_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "dev.chiraitori.anis.action.START"
        const val ACTION_STOP = "dev.chiraitori.anis.action.STOP"
    }
}
