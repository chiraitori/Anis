package dev.chiraitori.anis.data

import dev.chiraitori.anis.data.model.AdBlockStats
import dev.chiraitori.anis.data.model.AppQueryStat
import dev.chiraitori.anis.data.model.DnsQueryLog
import dev.chiraitori.anis.data.model.QueryStatus
import dev.chiraitori.anis.data.model.RuleCategory
import dev.chiraitori.anis.data.model.TopBlockedDomainStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

class QueryLogRepository {

    private val maxLogs = 500
    private val logDeque = ConcurrentLinkedDeque<DnsQueryLog>()

    private val _logsFlow = MutableStateFlow<List<DnsQueryLog>>(emptyList())
    val logsFlow: StateFlow<List<DnsQueryLog>> = _logsFlow.asStateFlow()

    private val _statsFlow = MutableStateFlow(AdBlockStats())
    val statsFlow: StateFlow<AdBlockStats> = _statsFlow.asStateFlow()

    private val _topBlockedDomainsFlow = MutableStateFlow<List<TopBlockedDomainStat>>(emptyList())
    val topBlockedDomainsFlow: StateFlow<List<TopBlockedDomainStat>> = _topBlockedDomainsFlow.asStateFlow()

    private val _topAppsFlow = MutableStateFlow<List<AppQueryStat>>(emptyList())
    val topAppsFlow: StateFlow<List<AppQueryStat>> = _topAppsFlow.asStateFlow()

    private val blockedDomainCounts = ConcurrentHashMap<String, AtomicLong>()
    private val appTotalCounts = ConcurrentHashMap<String, AtomicLong>()
    private val appBlockedCounts = ConcurrentHashMap<String, AtomicLong>()
    @Volatile
    private var retentionDays: Int = 7

    fun setRetentionDays(days: Int) {
        retentionDays = days.coerceAtLeast(0)
        if (retentionDays == 0) clearLogs() else pruneOldLogs(retentionDays)
    }

    fun logQuery(
        domain: String,
        queryType: String = "A",
        status: QueryStatus,
        blockReason: String? = null,
        latencyMs: Long = 0L,
        sourcePackage: String? = null,
        saveToLogs: Boolean = true
    ) {
        if (saveToLogs && retentionDays > 0) {
            val entry = DnsQueryLog(
                domain = domain,
                queryType = queryType,
                status = status,
                blockReason = blockReason,
                upstreamLatencyMs = latencyMs,
                sourcePackage = sourcePackage
            )

            logDeque.addFirst(entry)
            while (logDeque.size > maxLogs) {
                logDeque.removeLast()
            }
            val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60 * 60 * 1000L)
            logDeque.removeIf { it.timestamp < cutoff }
            _logsFlow.value = logDeque.toList()
        }

        // Update stats
        val currentStats = _statsFlow.value
        val isBlockedAd = status == QueryStatus.BLOCKED_AD
        val isBlockedFw = status == QueryStatus.BLOCKED_FIREWALL
        val isBlocked = isBlockedAd || isBlockedFw

        val category = if (isBlocked) resolveDomainCategory(domain) else null

        if (isBlocked) {
            blockedDomainCounts.computeIfAbsent(domain) { AtomicLong(0) }.incrementAndGet()
            recalculateTopBlocked()
        }

        if (sourcePackage != null && sourcePackage.isNotBlank()) {
            appTotalCounts.computeIfAbsent(sourcePackage) { AtomicLong(0) }.incrementAndGet()
            if (isBlocked) {
                appBlockedCounts.computeIfAbsent(sourcePackage) { AtomicLong(0) }.incrementAndGet()
            }
            recalculateTopApps()
        }

        _statsFlow.value = currentStats.copy(
            totalQueries = currentStats.totalQueries + 1,
            blockedQueries = if (isBlockedAd) currentStats.blockedQueries + 1 else currentStats.blockedQueries,
            blockedFirewall = if (isBlockedFw) currentStats.blockedFirewall + 1 else currentStats.blockedFirewall,
            adsCount = if (category == RuleCategory.ADS || isBlockedFw) currentStats.adsCount + 1 else currentStats.adsCount,
            trackersCount = if (category == RuleCategory.TRACKERS || category == RuleCategory.SOCIAL) currentStats.trackersCount + 1 else currentStats.trackersCount,
            malwareCount = if (category == RuleCategory.MALWARE) currentStats.malwareCount + 1 else currentStats.malwareCount,
            telemetryCount = if (category == RuleCategory.OEM_SPYWARE) currentStats.telemetryCount + 1 else currentStats.telemetryCount
        )
    }

    private fun recalculateTopBlocked() {
        val totalBlocked = _statsFlow.value.blockedQueries + _statsFlow.value.blockedFirewall
        val list = blockedDomainCounts.entries
            .sortedByDescending { it.value.get() }
            .take(10)
            .map { (domain, count) ->
                val c = count.get()
                val percentage = if (totalBlocked > 0) (c.toFloat() / totalBlocked.toFloat()) * 100f else 0f
                TopBlockedDomainStat(
                    domain = domain,
                    count = c,
                    category = resolveDomainCategory(domain),
                    percentage = percentage
                )
            }
        _topBlockedDomainsFlow.value = list
    }

    private fun recalculateTopApps() {
        val list = appTotalCounts.entries
            .sortedByDescending { it.value.get() }
            .take(8)
            .map { (pkg, totalCount) ->
                val blocked = appBlockedCounts[pkg]?.get() ?: 0L
                val appName = pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                AppQueryStat(
                    packageName = pkg,
                    appName = appName,
                    totalQueries = totalCount.get(),
                    blockedQueries = blocked
                )
            }
        _topAppsFlow.value = list
    }

    private fun resolveDomainCategory(domain: String): RuleCategory {
        return when {
            domain.contains("telemetry") || domain.contains("metrics") -> RuleCategory.OEM_SPYWARE
            domain.contains("malware") || domain.contains("phish") -> RuleCategory.MALWARE
            domain.contains("facebook") || domain.contains("tiktok") -> RuleCategory.SOCIAL
            domain.contains("analytics") || domain.contains("track") -> RuleCategory.TRACKERS
            else -> RuleCategory.ADS
        }
    }

    fun updateActiveRulesCount(count: Int) {
        _statsFlow.value = _statsFlow.value.copy(activeRulesCount = count)
    }

    fun clearLogs() {
        logDeque.clear()
        _logsFlow.value = emptyList()
    }

    fun pruneOldLogs(retentionDays: Int) {
        if (retentionDays <= 0) return
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60 * 60 * 1000L)
        logDeque.removeIf { it.timestamp < cutoff }
        _logsFlow.value = logDeque.toList()
    }

    fun resetStats() {
        blockedDomainCounts.clear()
        appTotalCounts.clear()
        appBlockedCounts.clear()
        _topBlockedDomainsFlow.value = emptyList()
        _topAppsFlow.value = emptyList()
        _statsFlow.value = AdBlockStats(
            totalQueries = 0,
            blockedQueries = 0,
            blockedFirewall = 0,
            activeRulesCount = _statsFlow.value.activeRulesCount
        )
    }

    companion object {
        val instance by lazy { QueryLogRepository() }
    }
}
