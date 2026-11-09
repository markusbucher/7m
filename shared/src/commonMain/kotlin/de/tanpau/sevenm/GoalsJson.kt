package de.tanpau.sevenm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Speichert/liest Torverteilungen als JSON (Cache und mitgeliefertes Startdaten-Set). */
object GoalsJson {
    fun decode(text: String): Map<String, ReportGoals> {
        val root = Json.parseToJsonElement(text) as? JsonObject ?: return emptyMap()
        val out = LinkedHashMap<String, ReportGoals>()
        for ((key, value) in root) {
            val o = value as? JsonObject ?: continue
            fun side(name: String): Map<String, PlayerGoals> {
                val s = o[name] as? JsonObject ?: return emptyMap()
                val m = LinkedHashMap<String, PlayerGoals>()
                for ((player, arr) in s) {
                    val a = arr as? JsonArray ?: continue
                    val goals = a.getOrNull(0)?.jsonPrimitive?.intOrNull ?: continue
                    m[player] = PlayerGoals(goals, a.getOrNull(1)?.jsonPrimitive?.intOrNull ?: 0)
                }
                return m
            }
            val home = side("h")
            val guest = side("a")
            val uh = o.optInt("uh")
            val ua = o.optInt("ua")
            out[key] = ReportGoals(
                home, guest, uh, ua,
                home.values.sumOf { it.goals } + uh,
                guest.values.sumOf { it.goals } + ua
            )
        }
        return out
    }

    fun encode(map: Map<String, ReportGoals>): String = buildJsonObject {
        for ((id, rg) in map) {
            put(id, buildJsonObject {
                put("h", side(rg.home))
                put("a", side(rg.guest))
                put("uh", rg.unassignedHome)
                put("ua", rg.unassignedGuest)
            })
        }
    }.toString()

    private fun side(m: Map<String, PlayerGoals>) = buildJsonObject {
        for ((n, pg) in m) put(n, buildJsonArray { add(JsonPrimitive(pg.goals)); add(JsonPrimitive(pg.sevenMeter)) })
    }

    private fun JsonObject.optInt(key: String): Int = (this[key] as? JsonPrimitive)?.intOrNull ?: 0
}
