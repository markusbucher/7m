package de.kirchheim.handball

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/** HTTP über Ktor; die Engine (OkHttp bzw. Darwin) wählt Ktor je Plattform selbst. */
class KtorFetcher(private val client: HttpClient = defaultClient()) : HttpFetcher {
    override suspend fun get(url: String): ByteArray {
        val response = client.get(url) { header(HttpHeaders.UserAgent, USER_AGENT) }
        if (!response.status.isSuccess()) throw IllegalStateException("HTTP ${response.status.value}")
        return response.readRawBytes()
    }

    companion object {
        const val USER_AGENT = "KirchheimHandball/2.0"

        fun defaultClient() = HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
            }
        }
    }
}
