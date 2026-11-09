package co.edu.sena.agrosoft.features.actividades.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.inicio.data.AprendizResumen
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import co.edu.sena.agrosoft.features.terreno.data.Lote
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

internal fun JsonElement?.comoNumero(): Double? =
    (this as? JsonPrimitive)?.contentOrNull?.trim()?.replace(",", ".")?.toDoubleOrNull()

@Serializable
data class Actividad(
    val id: Int,
    val nombre: String = "",
    val tipo: String = "",
    val subtipo: String? = null,
    val loteId: Int? = null,
    val cultivoId: Int? = null,
    val fecha: String? = null,
    val horasActividad: JsonElement? = null,
    val descripcion: String? = null,
    val estado: String = "Pendiente",
    val creadoPorUsuarioId: Int? = null,
) {
    val titulo: String get() = nombre.ifBlank { tipo }
    val activa: Boolean get() = estado == "Pendiente" || estado == "En_progreso"
}

@Serializable
data class PersonaAsignacion(
    val id: Int,
    val nombre: String = "",
    val apellido: String = "",
) {
    val completo: String get() = "$nombre $apellido".trim()
}

@Serializable
data class AsignacionActividad(
    val id: Int,
    val estado: String,
    val vencida: Boolean = false,
    val fechaLimite: String? = null,
    val horasTrabajadas: JsonElement? = null,
    val comentarioAprendiz: String? = null,
    val comentarioInstructor: String? = null,
    val entregadaEn: String? = null,
    val revisadaEn: String? = null,
    val aprendiz: PersonaAsignacion? = null,
) {
    val atrasada: Boolean get() = vencida && estado != "ENTREGADA" && estado != "APROBADA"
}

@Serializable
data class Cultivo(
    val id: Int,
    val nombreCultivo: String = "",
    val tipoCultivo: String? = null,
    val estado: String? = null,
)

@Serializable
private data class NuevaActividadDto(
    val nombre: String,
    val tipo: String,
    val loteId: Int,
    val cultivoId: Int,
    val fecha: String,
    val horasActividad: Double,
    val precioHoraActividad: Double,
    val descripcion: String? = null,
)

@Serializable
private data class AsignarDto(val aprendizIds: List<Int>, val fechaLimite: String? = null)

@Serializable
private data class EstadoDto(val estado: String)

class ActividadesRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun listar(): Resultado<List<Actividad>> = llamar { http.get("actividades") }

    suspend fun asignaciones(actividadId: Int): Resultado<List<AsignacionActividad>> =
        llamar { http.get("actividades/$actividadId/asignaciones") }

    suspend fun lotes(): List<Lote>? = opcional { http.get("territorio/lotes") }

    suspend fun cultivos(loteId: Int): Resultado<List<Cultivo>> = llamar { http.get("cultivos") { parameter("loteId", loteId) } }

    suspend fun aprendices(): List<AprendizResumen>? = opcional { http.get("aprendices/resumen") }

    suspend fun crear(
        nombre: String,
        tipo: String,
        loteId: Int,
        cultivoId: Int,
        fecha: String,
        horas: Double,
        valorHora: Double,
        descripcion: String?,
    ): Resultado<Actividad> = llamar {
        http.post("actividades") {
            setBody(NuevaActividadDto(nombre, tipo, loteId, cultivoId, fecha, horas, valorHora, descripcion))
        }
    }

    suspend fun asignar(actividadId: Int, aprendices: List<Int>, fechaLimite: String?): Resultado<Int> = try {
        val r = http.post("actividades/$actividadId/asignaciones") { setBody(AsignarDto(aprendices, fechaLimite)) }
        if (r.status.isSuccess()) {
            val cuerpo = runCatching { r.body<JsonObject>() }.getOrNull()
            Resultado.Ok(cuerpo?.get("asignadas")?.jsonArray?.size ?: aprendices.size)
        } else {
            Resultado.Falla(mensajeDe(r))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor.")
    }

    suspend fun quitar(actividadId: Int, asignacionId: Int): Resultado<Unit> =
        accion { http.delete("actividades/$actividadId/asignaciones/$asignacionId") }

    suspend fun cancelar(actividadId: Int): Resultado<Unit> =
        accion { http.patch("actividades/$actividadId/estado") { setBody(EstadoDto("Cancelada")) } }

    private suspend inline fun <reified T> llamar(bloque: suspend () -> HttpResponse): Resultado<T> = try {
        val r = bloque()
        when {
            r.status.isSuccess() -> Resultado.Ok(r.body<T>())
            r.status == HttpStatusCode.Forbidden -> Resultado.Falla("No tienes permiso para esta acción.")
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
