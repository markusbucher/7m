package de.tanpau.sevenm

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Liest die JSON-Antwort des Handball4All-Dienstes, den auch die BWHV-Seite benutzt. */
object ApiParser {
    const val LEAGUE_URL = "https://spo.handball4all.de/service/if_g_json.php?ca=1&cl=164126&cmd=ps&og=216"
    const val DEFAULT_REPORT_URL = "https://spo.handball4all.de/misc/sboPublicReports.php?sGID="

    private val whitespace = Regex("\\s+")
    private val halfTimeScore = Regex("""\(\d+:\d+\),?\s*""")
    private val wertungText = Regex("""gg\.\s*(Gast|Heim),?\s*""")

    fun parseLeague(text: String): League {
        val rootArray = Json.parseToJsonElement(text) as? JsonArray
            ?: throw IllegalStateException("Unerwartete Antwort des Verbands.")
        val root = rootArray.firstOrNull() as? JsonObject
            ?: throw IllegalStateException("Die Antwort des Verbands ist leer.")
        val head = root["head"] as? JsonObject ?: JsonObject(emptyMap())
        val content = root["content"] as? JsonObject
            ?: throw IllegalStateException("Die Antwort des Verbands enthält keine Liga-Daten.")

        val table = (content["score"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map { o ->
            TableRow(
                rank = o.int("tabScore"),
                team = clean(o.str("tabTeamname")),
                played = o.int("numPlayedGames"),
                won = o.int("numWonGames"),
                draw = o.int("numEqualGames"),
                lost = o.int("numLostGames"),
                goalsFor = o.int("numGoalsShot"),
                goalsAgainst = o.int("numGoalsGot"),
                pointsFor = o.int("pointsPlus"),
                pointsAgainst = o.int("pointsMinus")
            )
        }

        val games = LinkedHashMap<String, Game>()
        collectGames(content["futureGames"], games)
        collectGames(content["actualGames"], games)

        return League(
            name = head.str("name"),
            headline1 = head.str("headline1"),
            headline2 = head.str("headline2"),
            actualized = head.str("actualized"),
            reportUrlBase = head.str("repURL").ifEmpty { DEFAULT_REPORT_URL },
            table = table,
            games = games.values.sortedWith(compareBy<Game>({ it.date }, { it.time }, { it.id }))
        )
    }

    /** Spiele stehen je nach Antwort in einem Objekt mit "games" oder in einer Liste solcher Objekte. */
    private fun collectGames(node: JsonElement?, out: MutableMap<String, Game>) {
        when (node) {
            is JsonObject -> (node["games"] as? JsonArray).orEmpty().forEach { el ->
                val g = (el as? JsonObject)?.let(::parseGame) ?: return@forEach
                out[g.id] = g
            }
            is JsonArray -> node.forEach { collectGames(it, out) }
            else -> Unit
        }
    }

    internal fun parseGame(o: JsonObject): Game? {
        val id = o.str("gNo")
        if (id.isEmpty()) return null
        val date = parseDate(o.str("gDate")) ?: return null
        val hg = o.str("gHomeGoals").toIntOrNull()
        val ag = o.str("gGuestGoals").toIntOrNull()
        val played = hg != null && ag != null
        val comment = o.str("gComment")
        val wertung = when {
            !played -> Wertung.KEINE
            comment.contains("gg. Gast") -> Wertung.HEIM
            comment.contains("gg. Heim") -> Wertung.GAST
            else -> Wertung.KEINE
        }
        val note = comment
            .replace(halfTimeScore, "")
            .replace(wertungText, "")
            .trim()
            .trim(',')
            .trim()
        val sg = o.str("sGID")
        return Game(
            id = id,
            sgid = if (sg == "0") "" else sg,
            date = date,
            time = o.str("gTime"),
            hall = o.str("gGymnasiumNo"),
            hallName = o.str("gGymnasiumName"),
            home = clean(o.str("gHomeTeam")),
            guest = clean(o.str("gGuestTeam")),
            homeGoals = hg,
            guestGoals = ag,
            wertung = wertung,
            note = note
        )
    }

    /** "19.09.26" wird zu 19. September 2026. */
    internal fun parseDate(text: String): LocalDate? {
        val p = text.split(".")
        if (p.size < 3) return null
        val d = p[0].trim().toIntOrNull() ?: return null
        val m = p[1].trim().toIntOrNull() ?: return null
        var y = p[2].trim().toIntOrNull() ?: return null
        if (y < 100) y += 2000
        return try { LocalDate(y, m, d) } catch (e: IllegalArgumentException) { null }
    }

    private fun clean(s: String): String = s.replace(whitespace, " ").trim()

    private fun JsonObject.str(key: String): String = when (val v = this[key]) {
        null, JsonNull -> ""
        is JsonPrimitive -> v.content.trim()
        else -> v.toString().trim()
    }

    private fun JsonObject.int(key: String): Int = str(key).toIntOrNull() ?: 0
}
