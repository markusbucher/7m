package de.kirchheim.handball

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportParserTest {
    private val rg = ReportParser.parse(Fixtures.REPORT_LINES)!!

    @Test
    fun finalScoreFromLastGoal() {
        assertEquals(4, rg.finalHome)
        assertEquals(3, rg.finalGuest)
        assertTrue(rg.consistent)
    }

    @Test
    fun sideIsDerivedFromScore() {
        assertEquals(setOf("Max Muster", "Paul Probe"), rg.home.keys)
        assertEquals(setOf("Erika Beispiel"), rg.guest.keys)
    }

    @Test
    fun countsGoalsAndSevenMeters() {
        assertEquals(PlayerGoals(3, 1), rg.home["Max Muster"])
        assertEquals(PlayerGoals(1, 0), rg.home["Paul Probe"])
        assertEquals(PlayerGoals(2, 1), rg.guest["Erika Beispiel"], "verworfener 7m zählt nicht")
    }

    @Test
    fun goalsWithoutNameAreUnassigned() {
        assertEquals(0, rg.unassignedHome)
        assertEquals(1, rg.unassignedGuest)
        assertEquals(4, rg.sumHome)
        assertEquals(3, rg.sumGuest)
    }

    @Test
    fun repeatedLineAfterPageBreakIsCountedOnce() {
        // 2:2 steht im Fixture doppelt (Seitenumbruch); der Stand steigt dabei nicht.
        assertEquals(3, rg.home.getValue("Max Muster").goals)
    }

    @Test
    fun noGoalsGivesNull() {
        assertNull(ReportParser.parse(Fixtures.REPORT_WITHOUT_GOALS))
        assertNull(ReportParser.parse(emptyList()))
    }

    @Test
    fun inconsistentWhenScoreJumps() {
        // Ein Tor fehlt im Verlauf: Stand springt von 1:0 auf 3:0.
        val r = ReportParser.parse(
            listOf(
                "20:00:00 01:00 1:0 Tor durch A B (1, X)",
                "20:01:00 02:00 3:0 Tor durch A B (1, X)"
            )
        )!!
        assertEquals(3, r.finalHome)
        assertEquals(2, r.sumHome)
        assertEquals(false, r.consistent)
    }
}
