package dev.chiraitori.anis.data.model

enum class ProtectionMode(val title: String, val subtitle: String) {
    LOCAL_VPN("Local VPN Mode", "Standard rootless VPN tunnel filtering"),
    ROOT_PROXY("Root Proxy (iptables)", "Zero-overhead kernel DNS redirection")
}

enum class DnsResponseType(val title: String, val description: String) {
    ZERO_IP("Null IP (0.0.0.0)", "Fastest response; returns 0.0.0.0 to blocked requests"),
    NXDOMAIN("NXDOMAIN", "Returns 'Non-Existent Domain' to client"),
    REFUSED("REFUSED", "Returns DNS query refused error")
}

enum class AutoUpdateFrequency(val title: String, val days: Int) {
    DAILY("Every 24 Hours", 1),
    THREE_DAYS("Every 3 Days", 3),
    WEEKLY("Weekly", 7),
    MANUAL("Manual Only", 0)
}

enum class ThemeMode(val title: String) {
    SYSTEM("System Default"),
    DARK("Dark Theme"),
    LIGHT("Light Theme"),
    AMOLED("AMOLED Black")
}

enum class AppLanguage(val displayName: String, val languageCode: String) {
    SYSTEM("System Default", ""),
    ENGLISH("English", "en"),
    INDONESIAN("Bahasa Indonesia", "id"),
    JAPANESE("日本語 (Japanese)", "ja"),
    VIETNAMESE("Tiếng Việt (Vietnamese)", "vi"),
    CHINESE("中文 (Chinese)", "zh"),
    SPANISH("Español (Spanish)", "es"),
    GERMAN("Deutsch (German)", "de"),
    FRENCH("Français (French)", "fr"),
    RUSSIAN("Русский (Russian)", "ru")
}

enum class LogRetention(val title: String, val days: Int) {
    ONE_DAY("24 Hours", 1),
    SEVEN_DAYS("7 Days", 7),
    THIRTY_DAYS("30 Days", 30),
    NO_LOGS("Do Not Save Logs", 0)
}

enum class RuleType {
    BLOCK,
    ALLOW,
    COMMENT
}

enum class RuleCategory(val displayName: String, val iconName: String) {
    ADS("Ads & Banners", "Ads"),
    TRACKERS("Tracking & Telemetry", "Trackers"),
    MALWARE("Malware & Phishing", "Malware"),
    SOCIAL("Social Media Trackers", "Social"),
    OEM_SPYWARE("OEM & Device Telemetry", "Device"),
    CUSTOM("Custom Rules", "Custom")
}

enum class ProfileType(val title: String, val subtitle: String) {
    DEFAULT("Standard Protection", "Balanced ad & tracker blocking"),
    STRICT("Strict Shield", "Maximum blocking of tracking & telemetry"),
    FAMILY("Family Safe", "SafeSearch + Adult & Gambling filters"),
    GAMING("Gaming & Speed", "Low-latency essential ad filtering"),
    CUSTOM("Custom Profile", "Personalized protection rules")
}

data class ProtectionProfile(
    val id: String,
    val name: String,
    val profileType: ProfileType,
    val description: String,
    val enabledFilterIds: Set<String>,
    val safeSearchEnabled: Boolean = false,
    val youtubeRestrictedMode: Boolean = false,
    val isActive: Boolean = false
)

data class BlockListSource(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val isEnabled: Boolean = true,
    val ruleCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val category: RuleCategory = RuleCategory.ADS,
    val isCustom: Boolean = false
)

data class AppFirewallItem(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val isWhitelistedFromVpn: Boolean = false,
    val uid: Int = 0
)

enum class QueryStatus {
    ALLOWED,
    BLOCKED_AD,
    BLOCKED_FIREWALL,
    WHITELISTED,
    CUSTOM_REWRITE,
    SAFESEARCH_REDIRECT
}

data class DnsQueryLog(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val domain: String,
    val queryType: String = "A",
    val status: QueryStatus = QueryStatus.ALLOWED,
    val blockReason: String? = null,
    val upstreamLatencyMs: Long = 0L,
    val sourcePackage: String? = null
)

data class TopBlockedDomainStat(
    val domain: String,
    val count: Long,
    val category: RuleCategory = RuleCategory.ADS,
    val percentage: Float = 0f
)

data class AppQueryStat(
    val packageName: String,
    val appName: String,
    val totalQueries: Long = 0L,
    val blockedQueries: Long = 0L
)

data class AdBlockStats(
    val totalQueries: Long = 0L,
    val blockedQueries: Long = 0L,
    val blockedFirewall: Long = 0L,
    val activeRulesCount: Int = 0
) {
    val blockRate: Float
        get() = if (totalQueries > 0) (blockedQueries + blockedFirewall).toFloat() / totalQueries.toFloat() * 100f else 0f
}

enum class DnsCategory(val displayName: String) {
    STANDARD("Standard"),
    PRIVACY("Privacy & No-Logs"),
    SECURITY("Malware & Phishing Defense"),
    FAMILY("Family & Child Protection"),
    CUSTOM("Custom Endpoint")
}

enum class DnsProtocol(val displayName: String) {
    PLAIN_UDP("Plain DNS (UDP 53)"),
    DOH("DNS-over-HTTPS (Encrypted)")
}

data class CustomDnsRule(
    val id: String = "rule_${System.currentTimeMillis()}",
    val domain: String,
    val targetIp: String,
    val isEnabled: Boolean = true
)

data class UpstreamDnsProvider(
    val id: String,
    val name: String,
    val category: DnsCategory,
    val description: String,
    val primaryIp: String,
    val secondaryIp: String = "",
    val dohUrl: String? = null,
    val isCustom: Boolean = false
) {
    val isEncrypted: Boolean
        get() = !dohUrl.isNullOrBlank()
}

object DefaultDnsProviders {
    val CLOUDFLARE = UpstreamDnsProvider(
        id = "cloudflare",
        name = "Cloudflare DNS",
        category = DnsCategory.PRIVACY,
        description = "World's fastest privacy-first DNS (1.1.1.1)",
        primaryIp = "1.1.1.1",
        secondaryIp = "1.0.0.1",
        dohUrl = "https://cloudflare-dns.com/dns-query"
    )

    val GOOGLE = UpstreamDnsProvider(
        id = "google",
        name = "Google Public DNS",
        category = DnsCategory.STANDARD,
        description = "Global high-speed resolving (8.8.8.8)",
        primaryIp = "8.8.8.8",
        secondaryIp = "8.8.4.4",
        dohUrl = "https://dns.google/dns-query"
    )

    val QUAD9 = UpstreamDnsProvider(
        id = "quad9",
        name = "Quad9 Security",
        category = DnsCategory.SECURITY,
        description = "Enterprise-grade threat & malware intelligence (9.9.9.9)",
        primaryIp = "9.9.9.9",
        secondaryIp = "149.112.112.112",
        dohUrl = "https://dns.quad9.net/dns-query"
    )

    val ADGUARD_DNS = UpstreamDnsProvider(
        id = "adguard",
        name = "AdGuard DNS",
        category = DnsCategory.PRIVACY,
        description = "Upstream ad & tracker filtering server",
        primaryIp = "94.140.14.14",
        secondaryIp = "94.140.15.15",
        dohUrl = "https://dns.adguard-dns.com/dns-query"
    )

    val CLOUDFLARE_FAMILY = UpstreamDnsProvider(
        id = "cloudflare_family",
        name = "Cloudflare 1.1.1.3 Family",
        category = DnsCategory.FAMILY,
        description = "Blocks malware + adult content automatically",
        primaryIp = "1.1.1.3",
        secondaryIp = "1.0.0.3",
        dohUrl = "https://family.cloudflare-dns.com/dns-query"
    )

    val MULLVAD = UpstreamDnsProvider(
        id = "mullvad",
        name = "Mullvad DNS",
        category = DnsCategory.PRIVACY,
        description = "Strict no-logs audited Swedish privacy DNS",
        primaryIp = "194.242.2.2",
        secondaryIp = "194.242.2.3",
        dohUrl = "https://dns.mullvad.net/dns-query"
    )

    val OPENDNS_FAMILY = UpstreamDnsProvider(
        id = "opendns_family",
        name = "OpenDNS FamilyShield",
        category = DnsCategory.FAMILY,
        description = "Parental control and content safety",
        primaryIp = "208.67.222.123",
        secondaryIp = "208.67.220.123",
        dohUrl = null
    )

    val ALL = listOf(
        CLOUDFLARE,
        GOOGLE,
        QUAD9,
        ADGUARD_DNS,
        CLOUDFLARE_FAMILY,
        MULLVAD,
        OPENDNS_FAMILY
    )
}
