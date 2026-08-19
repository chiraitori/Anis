package dev.chiraitori.anis.data

import android.content.Context
import dev.chiraitori.anis.data.model.BlockListSource
import dev.chiraitori.anis.data.model.RuleCategory
import org.json.JSONObject

object DefaultBlockLists {

    /**
     * Dynamically loads blocklist source catalog from the bundled assets/sources.json file.
     * All source definitions and URLs are maintained in sources.json and synced from GitHub.
     */
    fun loadFromAssets(context: Context): List<BlockListSource> {
        return try {
            val jsonStr = context.assets.open("sources.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
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
            emptyList()
        }
    }
}
