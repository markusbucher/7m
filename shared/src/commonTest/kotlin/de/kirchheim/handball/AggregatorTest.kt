package de.kirchheim.handball

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun game(
    id: String,
    home: String = "SG HDKirchheim",
    guest: String = "TC Kurpfalz",
    hg: Int? = null,
    gg: Int? = null,
    wertung: Wertung = Wertung.KEINE,
    sgid: String = "s$id",
    day: Int = 1
) = Game(id, sgid, LocalDate(2026, 10, day), "20:00", "", "", home, guest, hg, gg, wertung, "")

internal fun goals(home: Map<String, PlayerGoals>, guest: Map<String, PlayerGoals>, uh: Int = 0, ua: Int = 0) =
    ReportGoals(home, guest, uh, ua, home.values.sumOf { it.goals } + uh, guest.values.sumOf { it.goals } + ua)

class AggregatorTest {
    @Test
    fun teamKeyIgnoresPunctuationAndCase() {
        assertEquals(Aggregator.teamKey("SG HD-Kirchheim"), Aggregator.teamKey("SG HDKirchheim"))
        assertEquals("hsgdielheimmalschenberg2", Aggregator.teamKey("HSG Dielheim/Malschenberg 2"))
        assertEquals("tcäöü", Aggregator.teamKey("TC ÄÖÜ"))
    }

    @Test
    fun scorersSumAcrossGamesAndKeepTeamsApart() {
        val games = listOf(game("1"), game("2", home = "TC Kurpfalz", guest = "SG HD-Kirchheim"), game("3"))
        val g = mapOf(
            "1" to goals(mapOf("Max Muster" to PlayerGoals(5, 2)), mapOf("Max Muster" to PlayerGoals(1, 0))),
            "2" to goals(mapOf("Erika Beispiel" to PlayerGoals(6, 0)), mapOf("Max Muster" to PlayerGoals(3, 1))),
            "x" to goals(mapOf("Nicht in Liga" to PlayerGoals(99, 0)), emptyMap())
        )
        val s = Aggregator.scorers(games, g)
        assertEquals(3, s.size, "gleicher Name in zwei Teams sind zwei Spieler; Spiele außerhalb der Liga zählen nicht")
        val max = s.first { it.name == "Max Muster" && Team.isOwn(it.team) }
        assertEquals(8, max.goals)
        assertEquals(3, max.sevenMeter)
        assertEquals(2, max.games)
        assertEquals(1, max.rank)
        assertEquals("Erika Beispiel", s[1].name)
        assertEquals(2, s[1].rank)
    }

    @Test
    fun equalGoalsShareRankFewerGamesFirst() {
        val games = listOf(game("1"), game("2"))
        val g = mapOf(
            "1" to goals(mapOf("A" to PlayerGoals(2, 0), "B" to PlayerGoals(4, 0)), mapOf("C" to PlayerGoals(1, 0))),
            "2" to goals(mapOf("A" to PlayerGoals(2, 0)), emptyMap())
        )
        val s = Aggregator.scorers(games, g)
        assertEquals(listOf("B" to 1, "A" to 1, "C" to 3), s.map { it.name to it.rank })
    }

    @Test
    fun validateAcceptsMatchingReport() {
        val rg = goals(mapOf("A" to PlayerGoals(2, 0)), mapOf("B" to PlayerGoals(1, 0)))
        assertNull(Aggregator.validate(game("1", hg = 2, gg = 1), rg))
    }

    @Test
    fun validateRejectsWrongResultOrSum() {
        val rg = goals(mapOf("A" to PlayerGoals(2, 0)), mapOf("B" to PlayerGoals(1, 0)))
        assertTrue(Aggregator.validate(game("1", hg = 3, gg = 1), rg)!!.contains("passt nicht"))
        val gap = rg.copy(finalHome = 3)
        assertTrue(Aggregator.validate(game("1", hg = 3, gg = 1), gap) != null)
    }

    @Test
    fun validateWertungOnlyChecksConsistency() {
        val rg = goals(mapOf("A" to PlayerGoals(20, 0)), mapOf("B" to PlayerGoals(18, 0)))
        assertNull(Aggregator.validate(game("1", hg = 0, gg = 0, wertung = Wertung.HEIM), rg))
        assertTrue(Aggregator.validate(game("1", hg = 0, gg = 0, wertung = Wertung.HEIM), rg.copy(finalHome = 21)) != null)
    }

    @Test
    fun validateIgnoresOpenGames() {
        assertNull(Aggregator.validate(game("1"), goals(emptyMap(), emptyMap())))
    }

    @Test
    fun unassignedOnlyForLeagueGames() {
        val g = mapOf("1" to goals(emptyMap(), emptyMap(), uh = 1, ua = 2), "x" to goals(emptyMap(), emptyMap(), uh = 5))
        assertEquals(3, Aggregator.unassigned(listOf(game("1"), game("2")), g))
    }
}
