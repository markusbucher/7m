package de.tanpau.sevenm

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

object Format {
    private val stamp = Regex("""\d{1,2}\.\d{1,2}\.\d{2,4},?\s*\d{1,2}:\d{2}\s*h?""")

    /** "Letzte Änderung: So, 08.11.2026, 15:25h" wird zu "08.11.2026, 15:25h"; Unbekanntes bleibt unverändert. */
    fun actualized(text: String): String = stamp.find(text)?.value ?: text

    private val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    /** 2026-09-19 wird zu "Sa 19.09.26". */
    fun date(d: LocalDate): String {
        val (y, m, day) = d.toString().split("-")
        return "${weekdays[d.dayOfWeek.isoDayNumber - 1]} $day.$m.${y.takeLast(2)}"
    }
}
