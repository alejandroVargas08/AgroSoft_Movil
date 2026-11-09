package co.edu.sena.agrosoft.features.revision.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class PersonaRevision(val id: Int, val nombre: String, val apellido: String? = null)

@Serializable
data class ActividadRevision(
    val id: Int,
    val nombre: String,
    val tipo: String? = null,
    val subtipo: String? = null,
    val descripcion: String? = null,
)

@Serializable
data class Entrega(
    val id: Int,
    val estado: String,
    val vencida: Boolean = false,
    val fechaLimite: String? = null,
    val horasTrabajadas: Double? = null,
    val comentarioAprendiz: String? = null,
    val entregadaEn: String? = null,
    val aprendiz: PersonaRevision? = null,
    val actividad: ActividadRevision? = null,
)

@Serializable
private data class ComentarioPeticion(val comentario: String? = null)

class RevisionRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun porRevisar(): Resultado<List<Entrega>> = llamar { http.get("asignaciones/por-revisar") }

    suspend fun aprobar(id: Int, comentario: String): Resultado<Entrega> = llamar {
        http.patch("asignaciones/$id/aprobar") { setBody(ComentarioPeticion(comentario.ifBlank { null })) }
    }

    suspend fun devolver(id: Int, comentario: String): Resultado<Entrega> = llamar {
        http.patch("asignaciones/$id/devolver") { setBody(ComentarioPeticion(comentario)) }
    }

    private suspend inline fun <reified T> llamar(bloque: suspend () -> HttpResponse): Resultado<T> = try {
        val respuesta = bloque()
        if (respuesta.status.isSuccess()) Resultado.Ok(respuesta.body<T>()) else Resultado.Falla(mensajeDe(respuesta))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
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
