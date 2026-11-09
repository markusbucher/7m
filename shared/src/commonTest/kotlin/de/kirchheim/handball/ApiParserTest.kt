package de.kirchheim.handball

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiParserTest {
    private val league = ApiParser.parseLeague(Fixtures.LEAGUE_JSON)

    @Test
    fun readsHead() {
        assertEquals("Männer Bezirksklasse Staffel 2", league.name)
        assertEquals("Männer Bezirksklasse Staffel 2 - Hallenrunde 2026/2027", league.headline2)
        assertEquals("https://example.invalid/report?sGID=", league.reportUrlBase)
    }

    @Test
    fun readsTableAndNormalizesWhitespace() {
        assertEquals(3, league.table.size)
        val first = league.table[0]
        assertEquals("SG Heidelberg-Leimen 2", first.team)
        assertEquals("152:115", first.goals)
        assertEquals("10:0", first.points)
        assertEquals("HSG Dielheim/Malschenberg 2", league.table[2].team)
        assertEquals(0, league.table[2].played, "null zählt als 0")
    }

    @Test
    fun numbersAsStringsAreAccepted() {
        assertEquals(4, league.table[1].played)
        assertTrue(league.table[1].isKirchheim)
        assertFalse(league.table[0].isKirchheim)
    }

    @Test
    fun collectsGamesFromObjectAndArrayAndSortsByDate() {
        assertEquals(listOf("808452", "808475", "808467", "808537", "808494"), league.games.map { it.id })
    }

    @Test
    fun skipsGamesWithoutNumberOrDate() {
        assertTrue(league.games.none { it.id == "999999" || it.home == "ohne Nummer" })
    }

    @Test
    fun playedGame() {
        val g = league.games.first { it.id == "808452" }
        assertEquals(LocalDate(2026, 9, 19), g.date)
        assertEquals("3501606", g.sgid)
        assertEquals(21, g.homeGoals)
        assertEquals(24, g.guestGoals)
        assertTrue(g.played)
        assertEquals(Wertung.KEINE, g.wertung)
        assertEquals("", g.note)
        assertEquals("Sportzentrum Süd Halle 1", g.hallName)
    }

    @Test
    fun futureGameHasNoResultAndNoReport() {
        val g = league.games.first { it.id == "808494" }
        assertFalse(g.played)
        assertNull(g.homeGoals)
        assertEquals("", g.sgid, "sGID 0 bedeutet: kein Bericht")
    }

    @Test
    fun wertungAgainstGuestAndNote() {
        val g = league.games.first { it.id == "808537" }
        assertEquals(Wertung.HEIM, g.wertung)
        assertEquals("Absage durch HSG DiMa", g.note)
        assertEquals(2, g.shownHome)
        assertEquals(0, g.shownGuest)
    }

    @Test
    fun parseDate() {
        assertEquals(LocalDate(2026, 9, 19), ApiParser.parseDate("19.09.26"))
        assertEquals(LocalDate(2027, 1, 2), ApiParser.parseDate("02.01.2027"))
        assertNull(ApiParser.parseDate("31.02.26"))
        assertNull(ApiParser.parseDate("19.09"))
        assertNull(ApiParser.parseDate(""))
    }

    @Test
    fun missingContentIsAnError() {
        assertFailsWith<IllegalStateException> { ApiParser.parseLeague("""[{"head":{}}]""") }
        assertFailsWith<IllegalStateException> { ApiParser.parseLeague("""[]""") }
        assertFailsWith<IllegalStateException> { ApiParser.parseLeague("""{"a":1}""") }
    }

    @Test
    fun defaultReportUrlWhenMissing() {
        val l = ApiParser.parseLeague("""[{"head":{},"content":{}}]""")
        assertEquals(ApiParser.DEFAULT_REPORT_URL, l.reportUrlBase)
        assertTrue(l.games.isEmpty())
        assertTrue(l.table.isEmpty())
    }
}
