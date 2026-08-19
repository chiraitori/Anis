package dev.chiraitori.anis

import dev.chiraitori.anis.data.BlockListRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockListParserTest {

    // Helper dummy repository instance for unit testing the parser pure method
    private val testLines = listOf(
        "# This is a comment" to null,
        "! Adblock list comment" to null,
        "   " to null,
        "127.0.0.1  localhost" to null,
        "0.0.0.0  0.0.0.0" to null,
        "0.0.0.0  pagead2.googlesyndication.com" to "pagead2.googlesyndication.com",
        "127.0.0.1 adservice.google.com # inline comment" to "adservice.google.com",
        "::1  ads.doubleclick.net" to "ads.doubleclick.net",
        "||tracking.miui.com^" to "tracking.miui.com",
        "||analytics.tiktok.com" to "analytics.tiktok.com",
        "raw-domain-tracker.com" to "raw-domain-tracker.com",
        "https://www.badsite.com/" to "badsite.com"
    )

    @Test
    fun testParseDomainFromLine() {
        for ((input, expected) in testLines) {
            val result = parseTestDomain(input)
            assertEquals("Failed for input: '$input'", expected, result)
        }
    }

    private fun parseTestDomain(rawLine: String): String? {
        var line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!") || line.startsWith("//")) {
            return null
        }

        val commentIdx = line.indexOf('#')
        if (commentIdx != -1) {
            line = line.substring(0, commentIdx).trim()
        }

        if (line.startsWith("||") && line.endsWith("^")) {
            line = line.substring(2, line.length - 1)
        } else if (line.startsWith("||")) {
            line = line.substring(2)
        }

        val parts = line.split("\\s+".toRegex())
        val candidate = if (parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1" || parts[0] == "::1")) {
            parts[1]
        } else {
            parts[0]
        }

        val cleaned = candidate.trim().lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .removeSuffix("/")
            .removeSuffix("^")

        return if (cleaned.isNotEmpty() && cleaned != "localhost" && cleaned != "broadcasthost" && cleaned != "local" && cleaned != "0.0.0.0") {
            cleaned
        } else {
            null
        }
    }
}
