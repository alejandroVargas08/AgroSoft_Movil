package co.edu.sena.agrosoft.features.sensores.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.coroutines.cancellation.CancellationException

internal fun JsonElement?.aNumero(): Double? =
    (this as? JsonPrimitive)?.contentOrNull?.trim()?.replace(",", ".")?.toDoubleOrNull()

enum class EstadoSensor { NORMAL, ALTO, BAJO, DESCONECTADO, INACTIVO, SIN_DATOS }

@Serializable
data class Sensor(
    val id: Int,
    val nombreSensor: String,
    val tipo: String? = null,
    val unidad: String? = null,
    val decimales: Int? = null,
    val valorMinimoSensor: JsonElement? = null,
    val valorMaximoSensor: JsonElement? = null,
    val activo: Boolean = true,
    val estadoConexion: String? = null,
    val ultimoValor: JsonElement? = null,
    val ultimaMedicion: String? = null,
    val ultimaVistaEn: String? = null,
    val loteId: Int? = null,
    val subLoteId: Int? = null,
) {
    val valorActual: Double? get() = ultimoValor.aNumero()
    val minimo: Double? get() = valorMinimoSensor.aNumero()
    val maximo: Double? get() = valorMaximoSensor.aNumero()
    val conectado: Boolean get() = estadoConexion == "conectado"

    val situacion: EstadoSensor
        get() {
            if (!activo) return EstadoSensor.INACTIVO
            if (!conectado) return EstadoSensor.DESCONECTADO
            val v = valorActual ?: return EstadoSensor.SIN_DATOS
            val max = maximo
            val min = minimo
            return when {
                max != null && v > max -> EstadoSensor.ALTO
                min != null && v < min -> EstadoSensor.BAJO
                else -> EstadoSensor.NORMAL
            }
        }

    val enAlerta: Boolean get() = situacion in setOf(EstadoSensor.ALTO, EstadoSensor.BAJO, EstadoSensor.DESCONECTADO)
}

@Serializable
data class Lectura(
    val id: Int? = null,
    val valor: JsonElement? = null,
    val fechaLectura: String? = null,
    val unidad: String? = null,
) {
    val numero: Double? get() = valor.aNumero()
}

@Serializable
data class AlertaSensor(
    val id: Int? = null,
    val valor: JsonElement? = null,
    val umbral: JsonElement? = null,
    val tipo: String? = null,
    val fechaAlerta: String? = null,
)

class SensoresRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun listar(): Resultado<List<Sensor>> = llamar { http.get("sensores") }

    suspend fun lecturas(sensorId: Int): Resultado<List<Lectura>> = llamar { http.get("sensor-lecturas/sensor/$sensorId") }

    suspend fun alertas(sensorId: Int): Resultado<List<AlertaSensor>> = llamar { http.get("sensor-alertas/sensor/$sensorId") }

    private suspend inline fun <reified T> llamar(bloque: suspend () -> HttpResponse): Resultado<T> = try {
        val r = bloque()
        when {
            r.status.isSuccess() -> Resultado.Ok(r.body<T>())
            r.status == HttpStatusCode.Forbidden -> Resultado.Falla("No tienes permiso para ver los sensores.")
            else -> Resultado.Falla("No se pudo cargar la información de los sensores.")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }
}
