package de.tanpau.sevenm

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/** Extrahiert den Text eines PDF-Spielberichts mit PDFBox (läuft lokal auf dem Handy). */
class PdfBoxTextExtractor(context: Context) : PdfTextExtractor {
    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    override fun lines(pdf: ByteArray): List<String> =
        PDDocument.load(pdf).use { doc ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = false
            stripper.getText(doc).lines()
        }
}
