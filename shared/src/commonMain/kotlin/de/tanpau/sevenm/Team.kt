package de.tanpau.sevenm

/** Die fest eingestellte eigene Mannschaft und Liga. */
object Team {
    const val DISPLAY_NAME = "SG HD-Kirchheim"

    /**
     * Vereinsnamen werden normalisiert verglichen: Tabelle ("SG HD-Kirchheim"), Spielplan und Bericht
     * ("SG HDKirchheim") schreiben den Namen unterschiedlich. Ein reiner Teilstring-Vergleich auf
     * "Kirchheim" würde auch andere Vereine (z. B. "TSV Kirchheim") treffen.
     */
    private val KEY = Aggregator.teamKey(DISPLAY_NAME)

    /** Erste Mannschaft der SG; "SG HD-Kirchheim 2" wäre ein anderes Team. */
    fun isOwn(name: String): Boolean = Aggregator.teamKey(name) == KEY
}
