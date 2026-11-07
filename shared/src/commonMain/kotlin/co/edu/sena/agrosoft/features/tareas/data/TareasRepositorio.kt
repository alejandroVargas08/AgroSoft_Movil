package co.edu.sena.agrosoft.features.tareas.data

import co.edu.sena.agrosoft.core.network.ApiCliente
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
data class ActividadTarea(
    val id: Int,
    val nombre: String,
    val tipo: String? = null,
    val subtipo: String? = null,
    val descripcion: String? = null,
    val fecha: String? = null,
)

@Serializable
data class PersonaTarea(val id: Int, val nombre: String, val apellido: String? = null)

@Serializable
data class Tarea(
    val id: Int,
    val estado: String,
    val vencida: Boolean = false,
    val fechaLimite: String? = null,
    val horasTrabajadas: Double? = null,
    val comentarioAprendiz: String? = null,
    val comentarioInstructor: String? = null,
    val entregadaEn: String? = null,
    val revisadaEn: String? = null,
    val asignadoPor: PersonaTarea? = null,
    val actividad: ActividadTarea? = null,
)

@Serializable
private data class EntregaPeticion(val horasTrabajadas: Double, val comentario: String? = null)

sealed interface Resultado<out T> {
    data class Ok<T>(val dato: T) : Resultado<T>
    data class Falla(val mensaje: String) : Resultado<Nothing>
}

class TareasRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun listar(): Resultado<List<Tarea>> = llamar { http.get("mis-tareas") }

    suspend fun iniciar(id: Int): Resultado<Tarea> = llamar { http.patch("mis-tareas/$id/iniciar") }

    suspend fun entregar(id: Int, horas: Double, comentario: String): Resultado<Tarea> = llamar {
        http.patch("mis-tareas/$id/entregar") {
            setBody(EntregaPeticion(horas, comentario.ifBlank { null }))
        }
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
