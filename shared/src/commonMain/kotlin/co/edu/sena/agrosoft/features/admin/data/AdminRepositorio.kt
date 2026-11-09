package co.edu.sena.agrosoft.features.admin.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

internal fun JsonElement?.cifra(): Double? =
    (this as? JsonPrimitive)?.contentOrNull?.trim()?.replace(",", ".")?.toDoubleOrNull()

@Serializable
data class SolicitudRegistro(
    val rolSolicitado: String? = null,
    val cargo: String? = null,
    val motivo: String? = null,
    val fichasACargo: List<Int> = emptyList(),
    val solicitadoEn: String? = null,
)

@Serializable
data class UsuarioAdmin(
    val id: Int,
    val nombre: String = "",
    val apellido: String = "",
    val identificacion: JsonElement? = null,
    val correo: String? = null,
    val telefono: String? = null,
    val estado: String? = null,
    val idFicha: String? = null,
    val solicitud: SolicitudRegistro? = null,
    val creadoEn: String? = null,
) {
    val nombreCompleto: String get() = "$nombre $apellido".trim()
    val documento: String? get() = (identificacion as? JsonPrimitive)?.contentOrNull
}

@Serializable
data class InstructorFicha(val id: Int, val nombre: String = "", val apellido: String = "")

@Serializable
data class FichaResumen(
    val id: Int,
    val nombre: String = "",
    val tipo: String? = null,
    val ficha: JsonElement? = null,
    val estado: String? = null,
    val instructores: List<InstructorFicha> = emptyList(),
    val totalAprendices: Int = 0,
) {
    val numero: String? get() = (ficha as? JsonPrimitive)?.contentOrNull
    val activa: Boolean get() = estado?.lowercase() !in setOf("inactivo", "inactiva", "finalizado", "finalizada", "cancelado")
}

@Serializable
data class InsumoBajo(
    val id: Int,
    val nombre: String = "",
    val unidadUso: String? = null,
    val stockDisponible: JsonElement? = null,
    val stockUso: JsonElement? = null,
    val stockMinimo: JsonElement? = null,
) {
    val disponible: Double get() = stockDisponible.cifra() ?: stockUso.cifra() ?: 0.0
    val minimo: Double get() = stockMinimo.cifra() ?: 0.0
}

@Serializable
private data class MotivoDto(val motivo: String)

class AdminRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun usuarios(): Resultado<List<UsuarioAdmin>> = llamar { http.get("usuarios") }

    suspend fun solicitudes(): Resultado<List<UsuarioAdmin>> = llamar { http.get("usuarios/solicitudes") }

    suspend fun fichas(): List<FichaResumen>? = opcional { http.get("programas-formacion/resumen") }

    suspend fun insumosBajos(): List<InsumoBajo>? = opcional { http.get("inventario/insumos/bajo-stock-minimo") }

    suspend fun aprobar(id: Int): Resultado<Unit> = accion { http.patch("usuarios/$id/aprobar") }

    suspend fun rechazar(id: Int, motivo: String): Resultado<Unit> =
        accion { http.patch("usuarios/$id/rechazar") { setBody(MotivoDto(motivo)) } }

    private suspend inline fun <reified T> llamar(bloque: suspend () -> HttpResponse): Resultado<T> = try {
        val r = bloque()
        when {
            r.status.isSuccess() -> Resultado.Ok(r.body<T>())
            r.status == HttpStatusCode.Forbidden -> Resultado.Falla("No tienes permiso para ver esta información.")
            else -> Resultado.Falla(mensajeDe(r))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }

    private suspend fun accion(bloque: suspend () -> HttpResponse): Resultado<Unit> = try {
        val r = bloque()
        if (r.status.isSuccess()) Resultado.Ok(Unit) else Resultado.Falla(mensajeDe(r))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor.")
    }

    private suspend inline fun <reified T> opcional(bloque: suspend () -> HttpResponse): T? = try {
        val r = bloque()
        if (r.status.isSuccess()) r.body<T>() else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
