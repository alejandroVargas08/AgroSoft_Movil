package co.edu.sena.agrosoft.features.tareas.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class Evidencia(
    val id: Int? = null,
    val descripcion: String? = null,
    val imagenes: List<String> = emptyList(),
)

class EvidenciasRepositorio(
    private val http: HttpClient = ApiCliente.http,
    private val subidas: HttpClient = ApiCliente.subidas,
) {

    suspend fun listar(actividadId: Int): Resultado<List<Evidencia>> =
        llamar { http.get("actividades/$actividadId/evidencias") }

    suspend fun subirFoto(actividadId: Int, jpeg: ByteArray, descripcion: String? = null): Resultado<Evidencia> = llamar {
        subidas.submitFormWithBinaryData(
            url = "actividades/$actividadId/evidencias/fotos",
            formData = formData {
                descripcion?.takeIf { it.isNotBlank() }?.let { append("descripcion", it) }
                append(
                    "foto",
                    jpeg,
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"evidencia.jpg\"")
                    },
                )
            },
        )
    }

    private suspend inline fun <reified T> llamar(bloque: suspend () -> HttpResponse): Resultado<T> = try {
        val respuesta = bloque()
        if (respuesta.status.isSuccess()) Resultado.Ok(respuesta.body<T>()) else Resultado.Falla(mensajeDe(respuesta))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo subir la foto. Revisa tu conexión.")
    }

    private suspend fun mensajeDe(respuesta: HttpResponse): String {
        val cuerpo = runCatching { respuesta.body<JsonObject>() }.getOrNull()
        return when (val m = cuerpo?.get("message")) {
            is JsonArray -> m.firstOrNull()?.jsonPrimitive?.contentOrNull
            null -> null
            else -> m.jsonPrimitive.contentOrNull
        } ?: "No se pudo completar la acción."
    }
}
