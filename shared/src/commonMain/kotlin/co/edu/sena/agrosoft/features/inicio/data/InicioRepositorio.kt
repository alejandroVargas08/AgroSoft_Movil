package co.edu.sena.agrosoft.features.inicio.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class ActividadResumen(
    val id: Int,
    val nombre: String,
    val tipo: String? = null,
    val loteId: Int? = null,
)
@Serializable
data class Asignacion(
    val id: Int,
    val estado: String,
    val vencida: Boolean = false,
    val fechaLimite: String? = null,
    val actividad: ActividadResumen? = null,
)

@Serializable
data class SensorDto(
    val id: Int,
    val nombreSensor: String? = null,
    val tipo: String? = null,
    val unidad: String? = null,
    val ultimoValor: JsonElement? = null,
    val ultimaMedicion: String? = null,
    val estadoConexion: String? = null,
    val valorMinimoSensor: JsonElement? = null,
    val valorMaximoSensor: JsonElement? = null,
)

@Serializable
data class UltimoAnalisisDto(
    val estado: String? = null,
    val saludablePct: JsonElement? = null,
)

@Serializable
data class VueloDto(
    val id: Int,
    val nombre: String,
    val fechaVuelo: String? = null,
    val loteId: Int? = null,
    val ultimoAnalisis: UltimoAnalisisDto? = null,
)

data class DatosInicioAprendiz(
    val tareas: List<Asignacion>?,
    val sensores: List<SensorDto>?,
    val vuelos: List<VueloDto>?,
)

fun JsonElement?.comoDouble(): Double? = (this as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()

class InicioRepositorio(private val http: HttpClient = ApiCliente.http) {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun cargarAprendiz(): DatosInicioAprendiz = coroutineScope {
        val tareas = async { obtener<List<Asignacion>>("mis-tareas") }
        val sensores = async { sensores() }
        val vuelos = async { obtener<List<VueloDto>>("vuelos") }
        DatosInicioAprendiz(tareas.await(), sensores.await(), vuelos.await())
    }

    private suspend fun sensores(): List<SensorDto>? {
        val crudo = obtener<JsonElement>("sensores") ?: return null
        val lista = when (crudo) {
            is JsonArray -> crudo
            is JsonObject -> (crudo["sensores"] ?: crudo["data"]) as? JsonArray
            else -> null
        } ?: return emptyList()
        return lista.mapNotNull { runCatching { json.decodeFromJsonElement(SensorDto.serializer(), it) }.getOrNull() }
    }

    private suspend inline fun <reified T> obtener(ruta: String): T? = try {
        val respuesta = http.get(ruta)
        if (respuesta.status.isSuccess()) respuesta.body<T>() else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
