package co.edu.sena.agrosoft.features.agrobot.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.core.network.ApiConfig
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class Conversacion(
    val id: Int,
    val titulo: String? = null,
    val creadoEn: String? = null,
    val actualizadoEn: String? = null,
)

@Serializable
data class MensajeIa(
    val id: Int? = null,
    val rol: String,
    val contenido: String,
    val tieneImagenes: Boolean = false,
    val cantidadImagenes: Int = 0,
    val creadoEn: String? = null,
)

@Serializable
data class RespuestaChat(val respuesta: String, val guardado: Boolean = false)

@Serializable
private data class CrearDto(val titulo: String)

@Serializable
private data class MensajeDto(val mensaje: String)

@Serializable
private data class ImagenesDto(val imagenes: List<String>, val prompt: String)

class AgroBotRepositorio {

    private val http = ApiCliente.http

    private val ia = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 200_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 200_000
        }
        defaultRequest {
            url(ApiConfig.BASE_URL)
            contentType(ContentType.Application.Json)
            ApiCliente.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

    suspend fun conversaciones(usuarioId: Int): Resultado<List<Conversacion>> =
        llamar { http.get("ia/conversaciones/usuario/$usuarioId") }

    suspend fun crear(titulo: String): Resultado<Conversacion> =
        llamar { http.post("ia/conversaciones") { setBody(CrearDto(titulo)) } }

    suspend fun mensajes(conversacionId: Int): Resultado<List<MensajeIa>> =
        llamar { http.get("ia/conversaciones/$conversacionId/mensajes") }

    suspend fun enviar(conversacionId: Int, mensaje: String): Resultado<RespuestaChat> =
        llamar { ia.post("agrobot/conversaciones/$conversacionId/chat") { setBody(MensajeDto(mensaje)) } }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analizarImagenes(conversacionId: Int, fotos: List<ByteArray>, prompt: String): Resultado<RespuestaChat> =
        llamar {
            ia.post("ia/conversaciones/$conversacionId/analizar-imagen") {
                setBody(ImagenesDto(fotos.map { Base64.Default.encode(it) }, prompt))
            }
        }

    suspend fun eliminar(conversacionId: Int): Resultado<Unit> = try {
        val r = http.delete("ia/conversaciones/$conversacionId")
        if (r.status.isSuccess()) Resultado.Ok(Unit) else Resultado.Falla(mensajeDe(r))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor.")
    }

    private suspend inline fun <reified T> llamar(bloque: suspend () -> HttpResponse): Resultado<T> = try {
        val r = bloque()
        if (r.status.isSuccess()) Resultado.Ok(r.body<T>()) else Resultado.Falla(mensajeDe(r))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("AgroBot no respondió a tiempo. Revisa tu conexión e intenta de nuevo.")
    }

    private suspend fun mensajeDe(respuesta: HttpResponse): String {
        val cuerpo = runCatching { respuesta.body<JsonObject>() }.getOrNull()
        return when (val m = cuerpo?.get("message")) {
            is JsonArray -> m.firstOrNull()?.jsonPrimitive?.contentOrNull
            null -> null
            else -> m.jsonPrimitive.contentOrNull
        } ?: "AgroBot no pudo responder en este momento."
    }
}
