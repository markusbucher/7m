package de.tanpau.sevenm

import android.content.Context
import java.io.File

/** Dateien im privaten App-Verzeichnis; Schreiben über eine temporäre Datei und Umbenennen. */
class FileStorage(private val dir: File) : Storage {
    override fun read(name: String): String? = File(dir, name).takeIf { it.exists() }?.readText()

    override fun write(name: String, text: String) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(File(dir, name))) {
            File(dir, name).writeText(text)
            tmp.delete()
        }
    }

    override fun delete(name: String) {
        File(dir, name).delete()
    }
}

fun androidPlatform(context: Context): Platform {
    val app = context.applicationContext
    return Platform(
        http = KtorFetcher(),
        storage = FileStorage(app.filesDir),
        pdf = PdfBoxTextExtractor(app),
        now = System::currentTimeMillis
    )
}
