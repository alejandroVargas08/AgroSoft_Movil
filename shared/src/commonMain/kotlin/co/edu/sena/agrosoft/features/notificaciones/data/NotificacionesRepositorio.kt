package co.edu.sena.agrosoft.features.notificaciones.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class Notificacion(
    val id: Int,
    val titulo: String,
    val mensaje: String,
    val tipo: String? = null,
    val leida: Boolean = false,
    val creadoEn: String? = null,
)

class NotificacionesRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun listar(usuarioId: Int): Resultado<List<Notificacion>> = llamar { http.get("notificaciones/$usuarioId") }

    suspend fun marcarLeida(id: Int): Resultado<Unit> = accion { http.patch("notificaciones/$id/leida") }

    suspend fun eliminar(id: Int): Resultado<Unit> = accion { http.delete("notificaciones/$id") }

    private suspend inline fun <reified T> llamar(bloque: suspend () -> HttpResponse): Resultado<T> = try {
        val respuesta = bloque()
        if (respuesta.status.isSuccess()) Resultado.Ok(respuesta.body<T>()) else Resultado.Falla(mensajeDe(respuesta))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }

    private suspend fun accion(bloque: suspend () -> HttpResponse): Resultado<Unit> = try {
        val respuesta = bloque()
        if (respuesta.status.isSuccess()) Resultado.Ok(Unit) else Resultado.Falla(mensajeDe(respuesta))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor.")
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
