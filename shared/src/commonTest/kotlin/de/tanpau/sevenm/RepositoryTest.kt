package de.tanpau.sevenm

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryTest {
    private val http = FakeHttp()
    private val storage = FakeStorage()
    private val clock = FakeClock()
    private val base = "https://example.invalid/report?sGID="
    private val leagueUrl = "https://example.invalid/league"

    /** 808452: 21:24, Bericht liefert 4:3 → passt nicht. Deshalb eigene Liga-Antwort mit passendem Ergebnis. */
    private val leagueJson = Fixtures.LEAGUE_JSON.replace("\"gHomeGoals\": \"21\", \"gGuestGoals\": \"24\"", "\"gHomeGoals\": \"4\", \"gGuestGoals\": \"3\"")
    private val report = Fixtures.REPORT_LINES.joinToString("\n").encodeToByteArray()

    private fun repo(seed: String? = null) =
        Repository(Platform(http, storage, TextPdf, clock::now), seed = { seed }, leagueUrl = leagueUrl)

    private fun serveLeague(json: String = leagueJson) { http.responses[leagueUrl] = { json.encodeToByteArray() } }

    @Test
    fun emptyCacheHasNoLeague() = runTest {
        val s = repo().loadCached()
        assertNull(s.league)
        assertTrue(s.goals.isEmpty())
        assertEquals(0L, s.updatedAtMillis)
    }

    @Test
    fun refreshLoadsLeagueAndEvaluatesReports() = runTest {
        serveLeague()
        http.responses[base + "3501606"] = { report }
        val s = repo().refresh()
        assertNotNull(s.league)
        assertEquals(PlayerGoals(3, 1), s.goals.getValue("808452").home["Max Muster"])
        assertEquals(clock.millis, s.updatedAtMillis)
        // 808467 (Wertung) und 808475 haben einen Bericht, der hier fehlt → Warnungen
        assertEquals(2, s.warnings.size)
        assertTrue(s.warnings.all { "nicht lesbar" in it })
    }

    @Test
    fun cachedStateSurvivesRestartIncludingWarnings() = runTest {
        serveLeague()
        http.responses[base + "3501606"] = { report }
        repo().refresh()
        val s = repo().loadCached()
        assertEquals(5, s.league!!.games.size)
        assertTrue("808452" in s.goals)
        assertEquals(2, s.warnings.size)
        assertEquals(clock.millis, s.updatedAtMillis)
    }

    @Test
    fun evaluatedReportsAreNotFetchedAgain() = runTest {
        serveLeague()
        http.responses[base + "3501606"] = { report }
        repo().refresh()
        http.calls.clear()
        clock.millis += Repository.RETRY_AFTER_MILLIS
        repo().refresh()
        assertTrue(base + "3501606" !in http.calls)
    }

    @Test
    fun failedReportIsRetriedOnlyAfterWaiting() = runTest {
        serveLeague()
        repo().refresh()
        assertTrue(base + "3501606" in http.calls)

        http.calls.clear()
        clock.millis += Repository.RETRY_AFTER_MILLIS - 1
        val s = repo().refresh()
        assertTrue(http.calls.none { it.startsWith(base) }, "kein erneuter Abruf vor Ablauf der Wartezeit")
        assertEquals(3, s.warnings.size, "Warnung bleibt sichtbar")

        http.calls.clear()
        clock.millis += 1
        http.responses[base + "3501606"] = { report }
        val s2 = repo().refresh()
        assertTrue(base + "3501606" in http.calls)
        assertTrue("808452" in s2.goals)
        assertEquals(2, s2.warnings.size)
    }

    @Test
    fun mismatchingReportIsRejected() = runTest {
        serveLeague(Fixtures.LEAGUE_JSON) // offiziell 21:24, Bericht 4:3
        http.responses[base + "3501606"] = { report }
        val s = repo().refresh()
        assertTrue("808452" !in s.goals)
        assertTrue(s.warnings.any { it.startsWith("Spiel 808452: Bericht 4:3 passt nicht zum Ergebnis 21:24") })
    }

    @Test
    fun reportWithoutGoalsGivesWarning() = runTest {
        serveLeague()
        http.responses[base + "3501606"] = { Fixtures.REPORT_WITHOUT_GOALS.joinToString("\n").encodeToByteArray() }
        val s = repo().refresh()
        assertTrue(s.warnings.any { it == "Spiel 808452: im Bericht keine Tore gefunden" })
    }

    @Test
    fun seedDataIsUsedAndNotFetched() = runTest {
        serveLeague()
        val seed = """{"808452": {"uh":0,"ua":0,"h":{"Seed Spieler":[4,0]},"a":{"Gast":[3,0]}}}"""
        val cached = repo(seed).loadCached()
        assertEquals(setOf("808452"), cached.goals.keys)
        val s = repo(seed).refresh()
        assertTrue(base + "3501606" !in http.calls)
        assertEquals(PlayerGoals(4, 0), s.goals.getValue("808452").home["Seed Spieler"])
    }

    @Test
    fun leagueErrorThrowsAndKeepsOldCache() = runTest {
        serveLeague()
        repo().refresh()
        val before = storage.files.toMap()
        http.responses[leagueUrl] = { throw IllegalStateException("HTTP 500") }
        clock.millis += 5
        assertFailsWith<IllegalStateException> { repo().refresh() }
        assertEquals(before, storage.files)
    }

    @Test
    fun brokenLeagueAnswerDoesNotOverwriteCache() = runTest {
        serveLeague()
        repo().refresh()
        val before = storage.files[Repository.LEAGUE_FILE]
        serveLeague("<html>Wartung</html>")
        assertFailsWith<Exception> { repo().refresh() }
        assertEquals(before, storage.files[Repository.LEAGUE_FILE])
    }

    @Test
    fun corruptCacheFilesAreIgnored() = runTest {
        storage.files[Repository.LEAGUE_FILE] = "kaputt"
        storage.files[Repository.GOALS_FILE] = "{"
        storage.files[Repository.FAILURES_FILE] = "[]"
        storage.files[Repository.META_FILE] = "x"
        val s = repo().loadCached()
        assertNull(s.league)
        assertTrue(s.goals.isEmpty())
        assertTrue(s.warnings.isEmpty())
    }

    @Test
    fun progressIsReported() = runTest {
        serveLeague()
        http.responses[base + "3501606"] = { report }
        val steps = ArrayList<String>()
        repo().refresh { steps += it }
        assertEquals("Lade Tabelle und Spielplan …", steps.first())
        assertEquals("Werte Spielbericht 3 von 3 aus …", steps.last())
    }
}
