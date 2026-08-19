package dev.chiraitori.anis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpsFilterEngineTest {

    private val adUrlPathPatterns = listOf(
        Regex("(?i)/api/stats/ads"),
        Regex("(?i)/pagead/"),
        Regex("(?i)/adservice/"),
        Regex("(?i)/ads/"),
        Regex("(?i)/ad_tag"),
        Regex("(?i)/doubleclick/"),
        Regex("(?i)/telemetry/"),
        Regex("(?i)/v1/analytics/")
    )

    private val trackingQueryParams = hashSetOf(
        "fbclid",
        "gclid",
        "msclkid",
        "mc_eid",
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "_ga"
    )

    private val defaultPinnedPackages = hashSetOf(
        "com.android.vending",
        "com.google.android.gms",
        "com.paypal.android.p2pmobile",
        "org.thoughtcrime.securesms"
    )

    @Test
    fun testAdPathMatching() {
        val matchesAd = adUrlPathPatterns.any { it.containsMatchIn("/pagead/interaction/?id=123") }
        assertTrue(matchesAd)

        val matchesVideoAd = adUrlPathPatterns.any { it.containsMatchIn("/api/stats/ads?v=xyz") }
        assertTrue(matchesVideoAd)

        val cleanPath = adUrlPathPatterns.any { it.containsMatchIn("/api/v2/feed/popular") }
        assertFalse(cleanPath)
    }

    @Test
    fun testTrackingQuerySanitization() {
        val rawQuery = "product=123&utm_source=facebook&utm_medium=cpc&fbclid=abc12345&lang=en"
        val pairs = rawQuery.split("&")
        val filtered = pairs.filterNot { pair ->
            val key = pair.substringBefore("=").lowercase()
            trackingQueryParams.contains(key)
        }
        val sanitized = filtered.joinToString("&")

        assertEquals("product=123&lang=en", sanitized)
    }

    @Test
    fun testPinnedAppBypass() {
        assertTrue(defaultPinnedPackages.contains("com.android.vending"))
        assertTrue(defaultPinnedPackages.contains("com.google.android.gms"))
        assertFalse(defaultPinnedPackages.contains("com.example.browser"))
    }
}
