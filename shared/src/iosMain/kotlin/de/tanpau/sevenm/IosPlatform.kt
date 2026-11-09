package de.tanpau.sevenm

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.PDFKit.PDFDocument

/** Extrahiert den Text eines PDF-Spielberichts mit Apples PDFKit (läuft lokal auf dem Gerät). */
@OptIn(ExperimentalForeignApi::class)
class PdfKitTextExtractor : PdfTextExtractor {
    override fun lines(pdf: ByteArray): List<String> {
        // Keine PDF-Datei (z. B. eine HTML-Fehlerseite des Servers): gar nicht erst an PDFKit geben.
        if (pdf.size < 5 || pdf.decodeToString(0, 5) != "%PDF-") throw IllegalStateException("Keine PDF-Datei")
        val doc = PDFDocument(data = pdf.toNSData())
        val out = ArrayList<String>()
        for (i in 0 until doc.pageCount.toInt()) {
            val page = doc.pageAtIndex(i.toULong()) ?: continue
            page.string?.let { out += it.lines() }
        }
        return out
    }
}

/** Dateien unter Application Support der App (wird nicht in „Dateien“ angezeigt). */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosFileStorage : Storage {
    private val dir: String by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .first() as String
        NSFileManager.defaultManager.createDirectoryAtPath(base, true, null, null)
        base
    }

    private fun path(name: String) = "$dir/$name"

    override fun read(name: String): String? =
        NSString.stringWithContentsOfFile(path(name), NSUTF8StringEncoding, null)

    override fun write(name: String, text: String) {
        val ok = NSString.create(string = text).writeToFile(path(name), atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!ok) throw IllegalStateException("Speichern fehlgeschlagen: $name")
    }

    override fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath(path(name), null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }

fun iosPlatform(): Platform = Platform(
    http = KtorFetcher(),
    storage = IosFileStorage(),
    pdf = PdfKitTextExtractor(),
    now = { (NSDate().timeIntervalSince1970 * 1000).toLong() }
)
