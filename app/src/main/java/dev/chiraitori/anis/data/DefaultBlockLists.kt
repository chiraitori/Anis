package dev.chiraitori.anis.data

import dev.chiraitori.anis.data.model.BlockListSource
import dev.chiraitori.anis.data.model.RuleCategory

object DefaultBlockLists {
    val SOURCES = listOf(
        BlockListSource(
            id = "adguard_base",
            name = "AdGuard DNS Filter",
            description = "Specialized filter composed of several filters (AdGuard Base, EasyList, etc.) optimized for DNS blocking.",
            url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
            isEnabled = true,
            ruleCount = 48000,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "steven_black",
            name = "Steven Black Unified",
            description = "Consolidated hosts list blocking adware, malware, fake sites, and tracking.",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            isEnabled = false,
            ruleCount = 65000,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "easylist",
            name = "EasyList",
            description = "The primary filter list that removes advert banners and tracking scripts.",
            url = "https://easylist.to/easylist/easylist.txt",
            isEnabled = false,
            ruleCount = 35000,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "easyprivacy",
            name = "EasyPrivacy",
            description = "Protection against web bugs, analytical trackers, and user monitoring scripts.",
            url = "https://easylist.to/easylist/easyprivacy.txt",
            isEnabled = false,
            ruleCount = 28000,
            category = RuleCategory.TRACKERS
        ),
        BlockListSource(
            id = "peter_lowe",
            name = "Peter Lowe's List",
            description = "Clean, curated list of known ad servers, web bugs, and analytical beacons.",
            url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
            isEnabled = false,
            ruleCount = 3800,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "fanboy_social",
            name = "Fanboy's Social Blocking",
            description = "Removes social media widgets, share buttons, and background trackers.",
            url = "https://easylist.to/easylist/fanboy-social.txt",
            isEnabled = false,
            ruleCount = 24000,
            category = RuleCategory.SOCIAL
        ),
        BlockListSource(
            id = "adguard_tracking",
            name = "AdGuard Tracking Protection",
            description = "Comprehensive defense against cross-site user telemetry and fingerprinting.",
            url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_3.txt",
            isEnabled = false,
            ruleCount = 18000,
            category = RuleCategory.TRACKERS
        ),
        BlockListSource(
            id = "oem_telemetry",
            name = "OEM & Device Telemetry",
            description = "Blocks background analytics and diagnostic reporting from device manufacturers.",
            url = "https://raw.githubusercontent.com/notracking/hosts-blocklists/master/hostnames.txt",
            isEnabled = false,
            ruleCount = 12000,
            category = RuleCategory.OEM_SPYWARE
        ),
        BlockListSource(
            id = "urlhaus_malware",
            name = "URLhaus Malware Defense",
            description = "Community database of active malware distribution and ransomware hosts.",
            url = "https://urlhaus.abuse.ch/downloads/hostfile/",
            isEnabled = false,
            ruleCount = 8500,
            category = RuleCategory.MALWARE
        ),
        BlockListSource(
            id = "hagezi_pro",
            name = "Hägezi Multi PRO",
            description = "High-accuracy multi-engine ad, tracking, and malware defense with zero false positives.",
            url = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.txt",
            isEnabled = false,
            ruleCount = 145000,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "oisd_big",
            name = "OISD Big Filter",
            description = "Industry standard anti-tracking and advert blocking list maintained by community telemetry.",
            url = "https://big.oisd.nl",
            isEnabled = false,
            ruleCount = 120000,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "one_hosts_lite",
            name = "1Hosts (Lite)",
            description = "Lightweight, highly effective mobile advertisement and tracker blocker.",
            url = "https://raw.githubusercontent.com/badmojr/1Hosts/master/Lite/hosts.txt",
            isEnabled = false,
            ruleCount = 75000,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "abpvn",
            name = "ABPVN Filter",
            description = "Specialized regional filter list blocking Vietnamese advertisements, popups, and banners.",
            url = "https://raw.githubusercontent.com/abpvn/abpvn/master/filter/abpvn.txt",
            isEnabled = false,
            ruleCount = 4500,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "hostsvn",
            name = "HostsVN",
            description = "Vietnamese ad servers, malicious domains, and scam prevention hosts.",
            url = "https://raw.githubusercontent.com/bigdargon/hostsVN/master/hosts",
            isEnabled = false,
            ruleCount = 18000,
            category = RuleCategory.ADS
        ),
        BlockListSource(
            id = "phishing_army",
            name = "Phishing Army Extended",
            description = "Real-time threat intelligence protecting against credential phishing and fraud.",
            url = "https://phishing.army/download/phishing_army_blocklist_extended.txt",
            isEnabled = false,
            ruleCount = 22000,
            category = RuleCategory.MALWARE
        ),
        BlockListSource(
            id = "steven_black_porn",
            name = "Adult & NSFW Filter",
            description = "Filters adult content, explicit domains, and malicious redirect sites.",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn-only/hosts",
            isEnabled = false,
            ruleCount = 32000,
            category = RuleCategory.MALWARE
        ),
        BlockListSource(
            id = "steven_black_gambling",
            name = "Gambling & Betting Filter",
            description = "Blocks online casino, betting, and gambling platforms.",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/gambling-only/hosts",
            isEnabled = false,
            ruleCount = 15000,
            category = RuleCategory.MALWARE
        )
    )

    /**
     * Dynamically loads default blocklist sources from the bundled assets/sources.json file.
     */
    fun loadFromAssets(context: android.content.Context): List<BlockListSource> {
        return try {
            val jsonStr = context.assets.open("sources.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONObject(jsonStr)
            val array = root.getJSONArray("sources")
            val list = mutableListOf<BlockListSource>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BlockListSource(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.getString("description"),
                        url = obj.getString("url"),
                        isEnabled = obj.optBoolean("isEnabled", false),
                        ruleCount = obj.optInt("ruleCount", 0),
                        lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                        category = try {
                            RuleCategory.valueOf(obj.optString("category", RuleCategory.ADS.name))
                        } catch (e: Exception) {
                            RuleCategory.ADS
                        },
                        isCustom = false
                    )
                )
            }
            list
        } catch (e: Exception) {
            SOURCES
        }
    }
}
