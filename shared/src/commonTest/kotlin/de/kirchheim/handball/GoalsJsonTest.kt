package de.kirchheim.handball

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoalsJsonTest {
    @Test
    fun roundTrip() {
        val m = mapOf(
            "1" to goals(mapOf("Max Muster" to PlayerGoals(5, 2)), mapOf("Erika Beispiel" to PlayerGoals(3, 0)), uh = 1, ua = 0),
            "2" to goals(emptyMap(), emptyMap())
        )
        val back = GoalsJson.decode(GoalsJson.encode(m))
        assertEquals(m, back)
        assertEquals(6, back.getValue("1").finalHome)
    }

    @Test
    fun readsSeedFormatAndIgnoresUnknownFields() {
        val text = """{"808452": {"sgid":"3501606","uh":0,"ua":4,"h":{"A":[8,2]},"a":{"B":[7]}}}"""
        val rg = GoalsJson.decode(text).getValue("808452")
        assertEquals(PlayerGoals(8, 2), rg.home["A"])
        assertEquals(PlayerGoals(7, 0), rg.guest["B"])
        assertEquals(11, rg.finalGuest)
        assertTrue(rg.consistent)
    }

    @Test
    fun skipsBrokenEntries() {
        val text = """{"1": 5, "2": {"h": {"A": "x", "B": [1, 0]}}}"""
        val m = GoalsJson.decode(text)
        assertEquals(setOf("2"), m.keys)
        assertEquals(setOf("B"), m.getValue("2").home.keys)
        assertTrue(GoalsJson.decode("[]").isEmpty())
    }
}
