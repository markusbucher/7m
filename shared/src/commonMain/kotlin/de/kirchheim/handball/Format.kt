package de.kirchheim.handball

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

object Format {
    private val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    /** 2026-09-19 wird zu "Sa 19.09.26". */
    fun date(d: LocalDate): String {
        val (y, m, day) = d.toString().split("-")
        return "${weekdays[d.dayOfWeek.isoDayNumber - 1]} $day.$m.${y.takeLast(2)}"
    }
}
