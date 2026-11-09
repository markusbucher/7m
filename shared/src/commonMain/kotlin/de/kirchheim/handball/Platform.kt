package de.kirchheim.handball

/** Lädt eine Adresse und liefert den Inhalt; wirft bei Netzwerk- oder HTTP-Fehlern. */
fun interface HttpFetcher {
    suspend fun get(url: String): ByteArray
}

/** Extrahiert den Text eines PDF-Spielberichts zeilenweise (Android: PDFBox, iOS: PDFKit). */
fun interface PdfTextExtractor {
    fun lines(pdf: ByteArray): List<String>
}

/** Einfacher Dateispeicher im privaten App-Verzeichnis. */
interface Storage {
    fun read(name: String): String?
    /** Schreibt atomar: Bei einem Absturz bleibt die alte Datei erhalten statt einer halben. */
    fun write(name: String, text: String)
    fun delete(name: String)
}

/** Alles, was die gemeinsame Logik von der jeweiligen Plattform braucht. */
class Platform(
    val http: HttpFetcher,
    val storage: Storage,
    val pdf: PdfTextExtractor,
    val now: () -> Long
)
