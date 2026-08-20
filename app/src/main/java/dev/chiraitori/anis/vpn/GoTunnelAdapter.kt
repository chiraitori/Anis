package dev.chiraitori.anis.vpn

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.QueryLogRepository
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.data.model.DnsProtocol
import dev.chiraitori.anis.data.model.DnsResponseType
import dev.chiraitori.anis.data.model.LogRetention
import dev.chiraitori.anis.data.model.QueryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tunnel.AppResolver
import tunnel.AppUidResolver
import tunnel.DomainChecker
import tunnel.FirewallChecker
import tunnel.SocketProtector
import tunnel.Tunnel
import tunnel.UIDResolver
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress

/** Bridges Anis settings/repositories to the vendored Go full-tunnel engine. */
class GoTunnelAdapter(
    private val context: Context,
    private val blockListRepository: BlockListRepository,
    private val settingsRepository: SettingsRepository,
    private val queryLogRepository: QueryLogRepository,
    private val scope: CoroutineScope,
    private val protectSocket: (Int) -> Boolean
) {
    private val engine = Tunnel.newEngine()
    private var engineJob: Job? = null

    @Volatile
    private var running = false

    fun start(vpnInterface: ParcelFileDescriptor, enableHttpsFiltering: Boolean) {
        if (running) return

        configureEngine(enableHttpsFiltering)
        running = true
        engineJob = scope.launch(Dispatchers.IO) {
            try {
                val protector = SocketProtector { fd -> protectSocket(fd.toInt()) }
                engine.startFull(vpnInterface.fd.toLong(), protector)
            } catch (error: Throwable) {
                if (running) Log.e(TAG, "Go tunnel stopped unexpectedly", error)
            } finally {
                running = false
            }
        }
    }

    fun stop() {
        if (!running && engineJob == null) return
        running = false
        try {
            engine.stop()
        } catch (error: Throwable) {
            Log.w(TAG, "Error stopping Go tunnel: ${error.message}")
        }
        engineJob?.cancel()
        engineJob = null
    }

    private fun configureEngine(enableHttpsFiltering: Boolean) {
        val upstream = settingsRepository.upstreamDnsFlow.value
        val protocol = if (
            settingsRepository.dnsProtocolFlow.value == DnsProtocol.DOH &&
            !upstream.dohUrl.isNullOrBlank()
        ) "DOH" else "PLAIN"

        engine.setDNS(
            protocol,
            upstream.primaryIp,
            upstream.secondaryIp,
            upstream.dohUrl.orEmpty()
        )
        engine.setBlockResponseType(
            when (settingsRepository.dnsResponseTypeFlow.value) {
                DnsResponseType.ZERO_IP -> "CUSTOM_IP"
                DnsResponseType.NXDOMAIN -> "NXDOMAIN"
                DnsResponseType.REFUSED -> "REFUSED"
            }
        )
        engine.setSafeSearch(settingsRepository.safeSearchEnabled)
        engine.setYouTubeRestricted(settingsRepository.youtubeRestrictedMode)
        engine.setConnLogEnabled(settingsRepository.logRetentionFlow.value != LogRetention.NO_LOGS)

        setupDomainChecker()
        setupUidResolvers()
        setupFirewallChecker()
        setupLogCallback()

        // Anis currently keeps parsed domains in Kotlin, so native trie paths are empty.
        engine.setTries("", "", "", "")

        if (enableHttpsFiltering) {
            val caDir = File(context.filesDir, "ca").apply { mkdirs() }
            engine.setUseTcpStack(true)
            val caPem = engine.startStackMitm(caDir.absolutePath)
            if (caPem.isBlank()) {
                Log.e(TAG, "The Go HTTPS filter could not initialize its certificate authority")
            } else {
                engine.setMitmAllowedUIDs(resolveBrowserUids())
                engine.setFilterHttp3(false)
                runCatching {
                    context.assets.open("https_passthrough.txt").bufferedReader().use { it.readText() }
                }.onSuccess(engine::setExtraPassthroughSuffixes)
                    .onFailure { Log.w(TAG, "Could not load HTTPS passthrough list", it) }
            }
        } else {
            engine.setUseTcpStack(false)
        }
    }

    private fun setupDomainChecker() {
        engine.setDomainChecker(object : DomainChecker {
            override fun isBlocked(domain: String): Boolean =
                blockListRepository.isDomainBlocked(normalizeDomain(domain))

            override fun getBlockReason(domain: String): String {
                val clean = normalizeDomain(domain)
                return when {
                    matchesDomainSet(clean, settingsRepository.blacklistFlow.value) -> "user blacklist"
                    blockListRepository.isDomainBlocked(clean) -> "blocklist"
                    else -> "custom"
                }
            }

            override fun hasCustomRule(domain: String): Long {
                val clean = normalizeDomain(domain)
                if (matchesDomainSet(clean, settingsRepository.whitelistFlow.value)) return 0L
                if (matchesDomainSet(clean, settingsRepository.blacklistFlow.value)) return 1L
                return -1L
            }

            override fun getRewriteIP(domain: String): String {
                val clean = normalizeDomain(domain)
                if (matchesDomainSet(clean, settingsRepository.whitelistFlow.value) ||
                    matchesDomainSet(clean, settingsRepository.blacklistFlow.value)
                ) return ""

                return settingsRepository.customRulesFlow.value.firstOrNull {
                    val ruleDomain = normalizeDomain(it.domain)
                    it.isEnabled && (clean == ruleDomain || clean.endsWith(".$ruleDomain"))
                }?.targetIp.orEmpty()
            }
        })
    }

    private fun setupUidResolvers() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        engine.setUIDResolver(UIDResolver { protocol, localIp, localPort, remoteIp, remotePort ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@UIDResolver -1L
            try {
                val ipProtocol = when (protocol.toInt()) {
                    6 -> OsConstants.IPPROTO_TCP
                    17 -> OsConstants.IPPROTO_UDP
                    else -> return@UIDResolver -1L
                }
                val local = InetSocketAddress(InetAddress.getByName(localIp), localPort.toInt())
                val remote = InetSocketAddress(InetAddress.getByName(remoteIp), remotePort.toInt())
                connectivityManager.getConnectionOwnerUid(ipProtocol, local, remote).toLong()
            } catch (_: Exception) {
                -1L
            }
        })

        engine.setAppUidResolver(AppUidResolver { uid -> packageForUid(uid.toInt()) })
        engine.setAppResolver(AppResolver { sourcePort, sourceIp, destinationIp, destinationPort ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@AppResolver ""
            try {
                val local = InetSocketAddress(InetAddress.getByAddress(sourceIp), sourcePort.toInt())
                val remote = InetSocketAddress(
                    InetAddress.getByAddress(destinationIp),
                    destinationPort.toInt()
                )
                val uid = connectivityManager.getConnectionOwnerUid(
                    OsConstants.IPPROTO_UDP,
                    local,
                    remote
                )
                packageForUid(uid)
            } catch (_: Exception) {
                ""
            }
        })
    }

    private fun setupFirewallChecker() {
        engine.setFirewallChecker(FirewallChecker { packageName ->
            packageName.isNotBlank() &&
                packageName in settingsRepository.firewallBlockedAppsFlow.value
        })
    }

    private fun setupLogCallback() {
        engine.setLogCallback { domain, blocked, queryType, responseTimeMs, appName, _, blockedBy ->
            val status = when {
                blocked && blockedBy.equals("firewall", ignoreCase = true) -> QueryStatus.BLOCKED_FIREWALL
                blocked -> QueryStatus.BLOCKED_AD
                else -> QueryStatus.ALLOWED
            }
            val saveLogs = settingsRepository.logRetentionFlow.value != LogRetention.NO_LOGS
            queryLogRepository.logQuery(
                domain = normalizeDomain(domain),
                queryType = dnsTypeName(queryType.toInt()),
                status = status,
                blockReason = blockedBy.ifBlank { null },
                latencyMs = responseTimeMs,
                sourcePackage = appName.ifBlank { null },
                saveToLogs = saveLogs
            )
        }
    }

    private fun resolveBrowserUids(): String {
        val packageManager = context.packageManager
        return BROWSER_PACKAGES.mapNotNull { packageName ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageUid(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageUid(packageName, 0)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }.distinct().joinToString(",")
    }

    private fun packageForUid(uid: Int): String = try {
        context.packageManager.getPackagesForUid(uid)?.firstOrNull().orEmpty()
    } catch (_: Exception) {
        ""
    }

    private fun normalizeDomain(domain: String): String =
        domain.trim().trimEnd('.').lowercase()

    private fun matchesDomainSet(domain: String, rules: Set<String>): Boolean {
        if (domain in rules) return true
        var dot = domain.indexOf('.')
        while (dot >= 0) {
            val parent = domain.substring(dot + 1)
            if (parent in rules || "*.$parent" in rules) return true
            dot = domain.indexOf('.', dot + 1)
        }
        return false
    }

    private fun dnsTypeName(type: Int): String = when (type) {
        1 -> "A"
        5 -> "CNAME"
        12 -> "PTR"
        15 -> "MX"
        16 -> "TXT"
        28 -> "AAAA"
        33 -> "SRV"
        65 -> "HTTPS"
        else -> "TYPE$type"
    }

    companion object {
        private const val TAG = "GoTunnelAdapter"

        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "com.brave.browser",
            "com.brave.browser_beta",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.opera.browser",
            "com.opera.browser.beta",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.kiwibrowser.browser",
            "org.cromite.cromite"
        )
    }
}
