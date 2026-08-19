package dev.chiraitori.anis

import dev.chiraitori.anis.data.model.RuleType
import dev.chiraitori.anis.vpn.CustomRuleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomRuleParserTest {

    @Test
    fun testParseStandardBlockRule() {
        val rule = CustomRuleParser.parseRule("||doubleclick.net^")
        assertNotNull(rule)
        assertEquals("doubleclick.net", rule!!.domain)
        assertEquals(RuleType.BLOCK, rule.ruleType)
        assertFalse(rule.isWildcard)
    }

    @Test
    fun testParseWildcardBlockRule() {
        val rule = CustomRuleParser.parseRule("||*.ads.example.com^")
        assertNotNull(rule)
        assertEquals("ads.example.com", rule!!.domain)
        assertEquals(RuleType.BLOCK, rule.ruleType)
        assertTrue(rule.isWildcard)
    }

    @Test
    fun testParseAllowRule() {
        val rule = CustomRuleParser.parseRule("@@||goodsite.com^")
        assertNotNull(rule)
        assertEquals("goodsite.com", rule!!.domain)
        assertEquals(RuleType.ALLOW, rule.ruleType)
        assertFalse(rule.isWildcard)
    }

    @Test
    fun testParseAllowWildcardRule() {
        val rule = CustomRuleParser.parseRule("@@||*.safesub.com^")
        assertNotNull(rule)
        assertEquals("safesub.com", rule!!.domain)
        assertEquals(RuleType.ALLOW, rule.ruleType)
        assertTrue(rule.isWildcard)
    }

    @Test
    fun testParseHostsFormat() {
        val rule1 = CustomRuleParser.parseRule("0.0.0.0 badsite.com")
        assertNotNull(rule1)
        assertEquals("badsite.com", rule1!!.domain)
        assertEquals(RuleType.BLOCK, rule1.ruleType)

        val rule2 = CustomRuleParser.parseRule("127.0.0.1 tracker.com # inline comment")
        assertNotNull(rule2)
        assertEquals("tracker.com", rule2!!.domain)
        assertEquals(RuleType.BLOCK, rule2.ruleType)
    }

    @Test
    fun testParseCommentsAndEmptyLines() {
        val comment1 = CustomRuleParser.parseRule("! Adblock Plus comment")
        assertNotNull(comment1)
        assertEquals(RuleType.COMMENT, comment1!!.ruleType)

        val comment2 = CustomRuleParser.parseRule("# Hosts file comment")
        assertNotNull(comment2)
        assertEquals(RuleType.COMMENT, comment2!!.ruleType)

        val empty = CustomRuleParser.parseRule("   ")
        assertNull(empty)
    }

    @Test
    fun testDomainMatching() {
        // Exact matching
        assertTrue(CustomRuleParser.matchesDomain("doubleclick.net", "doubleclick.net", false))
        assertTrue(CustomRuleParser.matchesDomain("ad.doubleclick.net", "doubleclick.net", false))

        // Wildcard matching
        assertTrue(CustomRuleParser.matchesDomain("sub.ads.example.com", "ads.example.com", true))
        assertFalse(CustomRuleParser.matchesDomain("other.com", "ads.example.com", true))
    }
}
