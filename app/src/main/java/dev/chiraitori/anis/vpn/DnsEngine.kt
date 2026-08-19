package dev.chiraitori.anis.vpn

import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.data.model.QueryStatus
import dev.chiraitori.anis.data.model.RuleCategory

sealed class DnsDecision {
    data class Block(
        val reason: String,
        val status: QueryStatus = QueryStatus.BLOCKED_AD,
        val category: RuleCategory = RuleCategory.ADS
    ) : DnsDecision()

    data class Rewrite(
        val ipAddress: String,
        val reason: String,
        val status: QueryStatus = QueryStatus.CUSTOM_REWRITE
    ) : DnsDecision()

    data class Allow(
        val reason: String = "Allowed",
        val status: QueryStatus = QueryStatus.ALLOWED
    ) : DnsDecision()
}

class DnsEngine(
    private val blockListRepository: BlockListRepository,
    private val settingsRepository: SettingsRepository
) {

    // SafeSearch IP mappings (standard documented VIPs)
    private val googleSafeSearchIp = "216.239.38.120"
    private val youtubeRestrictedIp = "216.239.38.119"
    private val bingSafeSearchIp = "204.79.197.220"
    private val duckDuckGoSafeSearchIp = "52.142.124.215"

    fun evaluate(domain: String): DnsDecision {
        val cleanDomain = domain.trim().lowercase()

        if (cleanDomain.isEmpty()) {
            return DnsDecision.Allow("Empty domain")
        }

        // 1. Check User Whitelist first (Direct & Wildcards)
        val whitelist = settingsRepository.whitelistFlow.value
        if (isDomainOrWildcardInSet(cleanDomain, whitelist)) {
            return DnsDecision.Allow("Whitelisted by user", QueryStatus.WHITELISTED)
        }

        // 2. Check User Custom Blacklist
        val blacklist = settingsRepository.blacklistFlow.value
        if (isDomainOrWildcardInSet(cleanDomain, blacklist)) {
            return DnsDecision.Block("User Custom Blacklist", QueryStatus.BLOCKED_AD, RuleCategory.CUSTOM)
        }

        // 3. Check SafeSearch if enabled in settings
        if (settingsRepository.safeSearchEnabled) {
            if (isGoogleSearchDomain(cleanDomain)) {
                return DnsDecision.Rewrite(googleSafeSearchIp, "SafeSearch (Google)", QueryStatus.SAFESEARCH_REDIRECT)
            }
            if (isBingDomain(cleanDomain)) {
                return DnsDecision.Rewrite(bingSafeSearchIp, "SafeSearch (Bing)", QueryStatus.SAFESEARCH_REDIRECT)
            }
            if (isDuckDuckGoDomain(cleanDomain)) {
                return DnsDecision.Rewrite(duckDuckGoSafeSearchIp, "SafeSearch (DuckDuckGo)", QueryStatus.SAFESEARCH_REDIRECT)
            }
        }

        if (settingsRepository.youtubeRestrictedMode && isYouTubeDomain(cleanDomain)) {
            return DnsDecision.Rewrite(youtubeRestrictedIp, "YouTube Restricted Mode", QueryStatus.SAFESEARCH_REDIRECT)
        }

        // 4. Check Custom DNS Rewrite Rules
        val customRules = settingsRepository.customRulesFlow.value
        val matchedRule = customRules.firstOrNull { it.isEnabled && (it.domain.equals(cleanDomain, ignoreCase = true) || isSubdomain(cleanDomain, it.domain)) }
        if (matchedRule != null) {
            return DnsDecision.Rewrite(matchedRule.targetIp, "Custom DNS Rewrite")
        }

        // 5. Check Active Blocklists
        val activeDomains = blockListRepository.getActiveBlockedDomains()
        if (isDomainOrSubdomainInSet(cleanDomain, activeDomains)) {
            val category = resolveCategoryForDomain(cleanDomain)
            return DnsDecision.Block("Ad & Tracker Blocklist", QueryStatus.BLOCKED_AD, category)
        }

        return DnsDecision.Allow()
    }

    private fun resolveCategoryForDomain(domain: String): RuleCategory {
        if (domain.contains("telemetry") || domain.contains("metrics") || domain.contains("miui") || domain.contains("xiaomi") || domain.contains("samsung")) {
            return RuleCategory.OEM_SPYWARE
        }
        if (domain.contains("malware") || domain.contains("phish") || domain.contains("virus") || domain.contains("trojan") || domain.contains("urlhaus")) {
            return RuleCategory.MALWARE
        }
        if (domain.contains("facebook") || domain.contains("tiktok") || domain.contains("twitter") || domain.contains("instagram")) {
            return RuleCategory.SOCIAL
        }
        if (domain.contains("analytics") || domain.contains("track") || domain.contains("adjust") || domain.contains("branch") || domain.contains("appsflyer")) {
            return RuleCategory.TRACKERS
        }
        return RuleCategory.ADS
    }

    private fun isGoogleSearchDomain(domain: String): Boolean {
        return domain == "google.com" || domain == "www.google.com" ||
                domain.startsWith("google.") || domain.contains(".google.")
    }

    private fun isYouTubeDomain(domain: String): Boolean {
        return domain == "youtube.com" || domain == "www.youtube.com" ||
                domain == "m.youtube.com" || domain == "youtubei.googleapis.com"
    }

    private fun isBingDomain(domain: String): Boolean {
        return domain == "bing.com" || domain == "www.bing.com"
    }

    private fun isDuckDuckGoDomain(domain: String): Boolean {
        return domain == "duckduckgo.com" || domain == "www.duckduckgo.com"
    }

    private fun isDomainOrSubdomainInSet(domain: String, set: Set<String>): Boolean {
        if (domain in set) return true

        var dotIndex = domain.indexOf('.')
        while (dotIndex != -1) {
            val parentDomain = domain.substring(dotIndex + 1)
            if (parentDomain in set) {
                return true
            }
            dotIndex = domain.indexOf('.', dotIndex + 1)
        }

        return false
    }

    private fun isDomainOrWildcardInSet(domain: String, set: Set<String>): Boolean {
        if (domain in set) return true

        var dotIndex = domain.indexOf('.')
        while (dotIndex != -1) {
            val parentDomain = domain.substring(dotIndex + 1)
            if (parentDomain in set || "*.$parentDomain" in set) {
                return true
            }
            dotIndex = domain.indexOf('.', dotIndex + 1)
        }

        return false
    }

    private fun isSubdomain(queryDomain: String, parentDomain: String): Boolean {
        return queryDomain.endsWith(".$parentDomain")
    }
}
