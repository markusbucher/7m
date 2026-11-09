package de.kirchheim.handball

class FakeStorage : Storage {
    val files = LinkedHashMap<String, String>()
    override fun read(name: String): String? = files[name]
    override fun write(name: String, text: String) { files[name] = text }
    override fun delete(name: String) { files.remove(name) }
}

/** Antwortet je URL mit festen Bytes oder wirft; zählt die Abrufe. */
class FakeHttp : HttpFetcher {
    val responses = LinkedHashMap<String, () -> ByteArray>()
    val calls = ArrayList<String>()
    override suspend fun get(url: String): ByteArray {
        calls += url
        return (responses[url] ?: throw IllegalStateException("HTTP 404")).invoke()
    }
}

/** "PDF" ist im Test einfach der Berichtstext. */
val TextPdf = PdfTextExtractor { it.decodeToString().lines() }

class FakeClock(var millis: Long = 1_000_000L) {
    fun now() = millis
}
