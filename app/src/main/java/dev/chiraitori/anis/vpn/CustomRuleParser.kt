package dev.chiraitori.anis.vpn

import dev.chiraitori.anis.data.model.RuleType

/**
 * Parsed custom DNS rule representation.
 */
data class ParsedRule(
    val rawText: String,
    val domain: String,
    val ruleType: RuleType,
    val isWildcard: Boolean = false
)

/**
 * Custom rule parser supporting standard Adblock Plus / uBlock Origin / hosts syntax.
 * Ported and enhanced from blockads-android.
 */
object CustomRuleParser {

    /**
     * Parses a single rule string.
     *
     * Supported formats:
     * - Block domain: `||example.com^` or `example.com`
     * - Block wildcard: `||*.ads.example.com^` or `*.ads.example.com`
     * - Allow / Exception: `@@||example.com^` or `@@example.com`
     * - Allow wildcard: `@@||*.example.com^`
     * - Hosts format: `0.0.0.0 ads.example.com` or `127.0.0.1 tracker.com`
     * - Comment: `! comment` or `# comment`
     */
    fun parseRule(rawLine: String): ParsedRule? {
        var line = rawLine.trim()
        if (line.isEmpty()) return null

        // 1. Comment line
        if (line.startsWith("!") || line.startsWith("#") || line.startsWith("//")) {
            return ParsedRule(rawText = line, domain = "", ruleType = RuleType.COMMENT)
        }

        // Strip trailing comment
        val commentIdx = line.indexOf('#')
        if (commentIdx != -1) {
            line = line.substring(0, commentIdx).trim()
            if (line.isEmpty()) return null
        }

        // 2. Allow rule: @@||example.com^ or @@example.com
        if (line.startsWith("@@||") && line.endsWith("^")) {
            val domain = line.removePrefix("@@||").removeSuffix("^").trim().lowercase()
            if (domain.isEmpty() || !isValidDomainOrWildcard(domain)) return null
            val isWildcard = domain.startsWith("*.")
            val cleanDomain = if (isWildcard) domain.removePrefix("*.") else domain
            return ParsedRule(rawText = rawLine, domain = cleanDomain, ruleType = RuleType.ALLOW, isWildcard = isWildcard)
        }

        if (line.startsWith("@@")) {
            val domain = line.removePrefix("@@").trim().lowercase().removeSuffix("^")
            if (domain.isEmpty() || !isValidDomainOrWildcard(domain)) return null
            val isWildcard = domain.startsWith("*.")
            val cleanDomain = if (isWildcard) domain.removePrefix("*.") else domain
            return ParsedRule(rawText = rawLine, domain = cleanDomain, ruleType = RuleType.ALLOW, isWildcard = isWildcard)
        }

        // 3. Block rule: ||example.com^
        if (line.startsWith("||") && line.endsWith("^")) {
            val domain = line.removePrefix("||").removeSuffix("^").trim().lowercase()
            if (domain.isEmpty() || !isValidDomainOrWildcard(domain)) return null
            val isWildcard = domain.startsWith("*.")
            val cleanDomain = if (isWildcard) domain.removePrefix("*.") else domain
            return ParsedRule(rawText = rawLine, domain = cleanDomain, ruleType = RuleType.BLOCK, isWildcard = isWildcard)
        }

        if (line.startsWith("||")) {
            val domain = line.removePrefix("||").trim().lowercase().removeSuffix("^")
            if (domain.isEmpty() || !isValidDomainOrWildcard(domain)) return null
            val isWildcard = domain.startsWith("*.")
            val cleanDomain = if (isWildcard) domain.removePrefix("*.") else domain
            return ParsedRule(rawText = rawLine, domain = cleanDomain, ruleType = RuleType.BLOCK, isWildcard = isWildcard)
        }

        // 4. Hosts format: 0.0.0.0 domain or 127.0.0.1 domain
        val tokens = line.split("\\s+".toRegex())
        val candidate = if (tokens.size >= 2 && (tokens[0] == "0.0.0.0" || tokens[0] == "127.0.0.1" || tokens[0] == "::1")) {
            tokens[1]
        } else {
            tokens[0]
        }

        val cleaned = candidate.trim().lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .removeSuffix("/")
            .removeSuffix("^")

        if (cleaned.isEmpty() || cleaned == "localhost" || cleaned == "broadcasthost" || cleaned == "local" || cleaned == "0.0.0.0") {
            return null
        }

        if (!isValidDomainOrWildcard(cleaned)) return null

        val isWildcard = cleaned.startsWith("*.")
        val cleanDomain = if (isWildcard) cleaned.removePrefix("*.") else cleaned
        return ParsedRule(rawText = rawLine, domain = cleanDomain, ruleType = RuleType.BLOCK, isWildcard = isWildcard)
    }

    /**
     * Parses multi-line rules text.
     */
    fun parseRules(text: String): List<ParsedRule> {
        return text.lineSequence()
            .mapNotNull { parseRule(it) }
            .filter { it.ruleType != RuleType.COMMENT }
            .toList()
    }

    /**
     * Validates domain string with wildcard support.
     */
    fun isValidDomainOrWildcard(domain: String): Boolean {
        if (domain.length < 3 || domain.length > 253) return false
        if (domain.startsWith(".") || domain.endsWith(".")) return false
        if (domain.contains("..") || domain.contains(" ") || domain.contains("/")) return false

        // Wildcard support e.g. *.ads.com or standard domain ads.com
        val domainRegex = Regex("^(\\*\\.)*[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$")
        return domainRegex.matches(domain)
    }

    /**
     * Matches a target query domain against a rule domain.
     * Supports exact match and subdomain wildcard matching.
     */
    fun matchesDomain(queryDomain: String, ruleDomain: String, isWildcard: Boolean = false): Boolean {
        val q = queryDomain.trim().lowercase()
        val r = ruleDomain.trim().lowercase()
        if (q == r) return true
        if (q.endsWith(".$r")) return true
        if (isWildcard && (q.endsWith(r) || q.contains(".$r"))) return true
        return false
    }
}
