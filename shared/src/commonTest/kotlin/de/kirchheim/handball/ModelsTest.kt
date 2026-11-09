package de.kirchheim.handball

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelsTest {
    @Test
    fun ownTeamMatchesAllSpellingsButNotOtherClubs() {
        assertTrue(Team.isOwn("SG HD-Kirchheim"))
        assertTrue(Team.isOwn("SG HDKirchheim"))
        assertTrue(Team.isOwn(" sg hd kirchheim "))
        assertFalse(Team.isOwn("TSV Kirchheim"))
        assertFalse(Team.isOwn("SG HD-Kirchheim 2"))
    }

    @Test
    fun outcomeFromKirchheimView() {
        assertEquals('S', game("1", hg = 30, gg = 20).outcomeForKirchheim())
        assertEquals('N', game("1", hg = 20, gg = 30).outcomeForKirchheim())
        assertEquals('U', game("1", hg = 25, gg = 25).outcomeForKirchheim())
        assertEquals('S', game("1", home = "TC Kurpfalz", guest = "SG HD-Kirchheim", hg = 20, gg = 30).outcomeForKirchheim())
        assertNull(game("1").outcomeForKirchheim(), "offenes Spiel")
        assertNull(game("1", home = "A", guest = "B", hg = 1, gg = 0).outcomeForKirchheim(), "ohne Kirchheim")
    }

    @Test
    fun wertungOverridesShownScore() {
        val heim = game("1", home = "TC Kurpfalz", hg = 0, gg = 0, guest = "SG HDKirchheim", wertung = Wertung.HEIM)
        assertEquals(2, heim.shownHome)
        assertEquals(0, heim.shownGuest)
        assertEquals('N', heim.outcomeForKirchheim())
        val gast = game("1", hg = 0, gg = 0, guest = "X", wertung = Wertung.GAST)
        assertEquals(0, gast.shownHome)
        assertEquals(2, gast.shownGuest)
        assertEquals('N', gast.outcomeForKirchheim())
    }

    @Test
    fun formatDate() {
        assertEquals("Sa 19.09.26", Format.date(LocalDate(2026, 9, 19)))
        assertEquals("Mo 01.03.27", Format.date(LocalDate(2027, 3, 1)))
        assertEquals("So 08.11.26", Format.date(LocalDate(2026, 11, 8)))
    }
}
