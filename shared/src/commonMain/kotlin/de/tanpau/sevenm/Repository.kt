package de.tanpau.sevenm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Holt Tabelle/Spielplan vom Handball4All-Dienst und wertet neue Spielberichte (PDF) direkt auf dem
 * Gerät aus. Plattformabhängiges (Netz, Dateien, PDF) kommt über [Platform] herein.
 *
 * Blockierende Aufrufe (Dateien, PDF): vom Aufrufer auf einem Hintergrund-Dispatcher ausführen.
 */
class Repository(
    private val platform: Platform,
    private val seed: suspend () -> String? = { null },
    private val leagueUrl: String = ApiParser.LEAGUE_URL
) {
    private val storage get() = platform.storage

    /** Gespeicherter Fehlversuch für einen Spielbericht. */
    internal data class Failure(val atMillis: Long, val message: String)

    private suspend fun seedGoals(): Map<String, ReportGoals> = try {
        seed()?.let(GoalsJson::decode).orEmpty()
    } catch (e: Exception) { emptyMap() }

    private fun cachedGoals(): Map<String, ReportGoals> = try {
        storage.read(GOALS_FILE)?.let(GoalsJson::decode).orEmpty()
    } catch (e: Exception) { emptyMap() }

    private fun cachedLeague(): League? = try {
        storage.read(LEAGUE_FILE)?.let(ApiParser::parseLeague)
    } catch (e: Exception) { null }

    private fun updatedAt(): Long = storage.read(META_FILE)?.let {
        try { ((Json.parseToJsonElement(it) as JsonObject)["updatedAt"] as JsonPrimitive).longOrNull } catch (e: Exception) { null }
    } ?: 0L

    internal fun failures(): Map<String, Failure> = try {
        val root = storage.read(FAILURES_FILE)?.let { Json.parseToJsonElement(it) as JsonObject } ?: JsonObject(emptyMap())
        root.mapNotNull { (id, v) ->
            val o = v as? JsonObject ?: return@mapNotNull null
            val t = (o["t"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
            val w = (o["w"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            id to Failure(t, w)
        }.toMap()
    } catch (e: Exception) { emptyMap() }

    private fun saveFailures(map: Map<String, Failure>) {
        storage.write(FAILURES_FILE, buildJsonObject {
            for ((id, f) in map) put(id, buildJsonObject { put("t", f.atMillis); put("w", f.message) })
        }.toString())
    }

    /** Lädt den zuletzt gespeicherten Stand ohne Netzwerk, inklusive der Warnungen des letzten Abrufs. */
    suspend fun loadCached(): AppState {
        val league = cachedLeague()
        val goals = seedGoals() + cachedGoals()
        val warnings = failures().filterKeys { it !in goals }.values.map { it.message }
        return AppState(league, goals, warnings, if (league != null) updatedAt() else 0L)
    }

    /**
     * Netzwerk-Aktualisierung. Wirft bei Fehlern der Liga-Abfrage; Fehler einzelner Spielberichte
     * werden gespeichert und als Warnungen geliefert. Ein fehlgeschlagener Bericht wird erst nach
     * [RETRY_AFTER_MILLIS] erneut geladen, statt bei jedem Tippen auf „Aktualisieren“.
     */
    suspend fun refresh(onProgress: (String) -> Unit = {}): AppState {
        onProgress("Lade Tabelle und Spielplan …")
        val text = platform.http.get(leagueUrl).decodeToString()
        val league = ApiParser.parseLeague(text)
        storage.write(LEAGUE_FILE, text)

        val goals = LinkedHashMap<String, ReportGoals>()
        goals.putAll(seedGoals())
        goals.putAll(cachedGoals())
        val now = platform.now()
        val failures = failures().filterKeys { it !in goals }.toMutableMap()

        val todo = league.games.filter { game ->
            game.played && game.sgid.isNotEmpty() && game.id !in goals &&
                failures[game.id].let { it == null || now - it.atMillis >= RETRY_AFTER_MILLIS }
        }
        for ((index, game) in todo.withIndex()) {
            onProgress("Werte Spielbericht ${index + 1} von ${todo.size} aus …")
            val problem = try {
                val lines = platform.pdf.lines(platform.http.get(league.reportUrlBase + game.sgid))
                val rg = ReportParser.parse(lines)
                if (rg == null) {
                    "Spiel ${game.id}: im Bericht keine Tore gefunden"
                } else {
                    Aggregator.validate(game, rg) ?: run { goals[game.id] = rg; null }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                "Spiel ${game.id}: Bericht nicht lesbar (${e::class.simpleName})"
            }
            if (problem != null) failures[game.id] = Failure(now, problem) else failures.remove(game.id)
        }
        storage.write(GOALS_FILE, GoalsJson.encode(goals))
        saveFailures(failures)
        storage.write(META_FILE, buildJsonObject { put("updatedAt", now) }.toString())
        return AppState(league, goals, failures.values.map { it.message }, now)
    }

    companion object {
        // Dateinamen wie in Version 1, damit ein Update den gespeicherten Stand übernimmt.
        const val LEAGUE_FILE = "league.json"
        const val GOALS_FILE = "goals_cache.json"
        const val FAILURES_FILE = "report_failures.json"
        const val META_FILE = "meta.json"
        const val RETRY_AFTER_MILLIS = 12L * 60 * 60 * 1000
    }
}
