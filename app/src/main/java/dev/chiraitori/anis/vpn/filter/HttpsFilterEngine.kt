package dev.chiraitori.anis.vpn.filter

import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.SettingsRepository
import java.net.URI

class HttpsFilterEngine(
    private val blockListRepository: BlockListRepository,
    private val settingsRepository: SettingsRepository
) {

    // Package names of apps known to strictly enforce SSL certificate pinning
    private val defaultPinnedPackages = hashSetOf(
        "com.android.vending",               // Google Play Store
        "com.google.android.gms",           // Google Play Services
        "com.google.android.gsf",           // Google Services Framework
        "com.google.android.apps.authenticator2", // Authenticator
        "com.google.android.apps.walletnfcrel",  // Google Wallet
        "com.paypal.android.p2pmobile",     // PayPal
        "com.chase.sig.android",            // Chase Bank
        "com.bankofamerica.mobilebanking",  // BofA
        "com.wf.wellsfargomobile",          // Wells Fargo
        "com.citi.citimobile",              // Citi
        "org.telegram.messenger",           // Telegram
        "org.thoughtcrime.securesms",        // Signal
        "com.whatsapp",                     // WhatsApp
        "com.spotify.music"                 // Spotify
    )

    // Domains that use strict SSL pinning / system security
    private val defaultPinnedDomains = hashSetOf(
        "play.google.com",
        "play.googleapis.com",
        "android.clients.google.com",
        "googleapis.com",
        "gstatic.com",
        "apple.com",
        "icloud.com",
        "identity.apple.com",
        "paypal.com",
        "paypalobjects.com"
    )

    // Known URL path patterns for ads, trackers, and telemetry
    private val adUrlPathPatterns = listOf(
        Regex("(?i)/api/stats/ads"),
        Regex("(?i)/pagead/"),
        Regex("(?i)/adservice/"),
        Regex("(?i)/ads/"),
        Regex("(?i)/ad_tag"),
        Regex("(?i)/ad_log"),
        Regex("(?i)/adx/"),
        Regex("(?i)/doubleclick/"),
        Regex("(?i)/facebook/tr/"),
        Regex("(?i)/g/collect(\\?|$)"),
        Regex("(?i)/telemetry/"),
        Regex("(?i)/v1/analytics/"),
        Regex("(?i)/metrics/collect"),
        Regex("(?i)/beacon/"),
        Regex("(?i)/pixel\\.gif")
    )

    // Tracking query parameters to strip
    private val trackingQueryParams = hashSetOf(
        "fbclid",
        "gclid",
        "msclkid",
        "mc_eid",
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "_ga",
        "_gl",
        "yclid",
        "dclid"
    )

    /**
     * Determines whether an application package should bypass HTTPS MITM decryption.
     */
    fun shouldBypassApp(packageName: String): Boolean {
        if (defaultPinnedPackages.contains(packageName)) return true
        val userBypassed = settingsRepository.whitelistedAppsFlow.value
        return userBypassed.contains(packageName)
    }

    /**
     * Determines whether a host should bypass HTTPS MITM decryption.
     */
    fun shouldBypassHost(host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        if (defaultPinnedDomains.contains(cleanHost)) return true
        return defaultPinnedDomains.any { cleanHost.endsWith(".$it") }
    }

    /**
     * Checks if a request path or URL matches ad/tracker blocking patterns.
     */
    fun shouldBlockUrl(host: String, path: String): Boolean {
        // 1. Check if the domain itself is in blocklists
        if (blockListRepository.isDomainBlocked(host)) {
            return true
        }

        // 2. Check path against known ad/telemetry regex patterns
        for (pattern in adUrlPathPatterns) {
            if (pattern.containsMatchIn(path)) {
                return true
            }
        }

        return false
    }

    /**
     * Strips intrusive tracking parameters (e.g. utm_*, fbclid, gclid) from a URL query string.
     */
    fun sanitizeQuery(rawQuery: String?): String? {
        if (rawQuery.isNullOrEmpty()) return rawQuery
        val pairs = rawQuery.split("&")
        val filtered = pairs.filterNot { pair ->
            val key = pair.substringBefore("=").lowercase()
            trackingQueryParams.contains(key)
        }
        return if (filtered.isEmpty()) null else filtered.joinToString("&")
    }
}
