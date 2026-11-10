package co.edu.sena.agrosoft.core.network

import co.edu.sena.agrosoft.Entorno
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiConfig {
    const val BASE_URL = Entorno.API_URL
}

object ApiCliente {

    var token: String? = null

    private val formatoJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    val http = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(formatoJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(ApiConfig.BASE_URL)
            contentType(ContentType.Application.Json)
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

    val subidas = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(formatoJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(ApiConfig.BASE_URL)
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
