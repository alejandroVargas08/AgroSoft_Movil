package co.edu.sena.agrosoft.features.inicio.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class AprendizResumen(
    val id: Int,
    val nombre: String,
    val apellido: String = "",
    val idFicha: String? = null,
    val tareas: Int = 0,
    val aprobadas: Int = 0,
    val entregadas: Int = 0,
    val vencidas: Int = 0,
    val horas: JsonElement? = null,
    val ultimoIngreso: String? = null,
) {
    val nombreCompleto: String get() = "$nombre $apellido".trim()
    val horasTotales: Double get() = horas.comoDouble() ?: 0.0
}

@Serializable
data class SensorEstado(
    val id: Int? = null,
    val activo: Boolean = true,
    val estadoConexion: String? = null,
    val ultimoValor: String? = null,
    val valorMinimoSensor: JsonElement? = null,
    val valorMaximoSensor: JsonElement? = null,
) {
    val enAlerta: Boolean
        get() {
            if (!activo) return false
            if (estadoConexion != "conectado") return true
            val valor = ultimoValor?.replace(",", ".")?.toDoubleOrNull() ?: return false
            val minimo = valorMinimoSensor.comoDouble()
            val maximo = valorMaximoSensor.comoDouble()
            return (minimo != null && valor < minimo) || (maximo != null && valor > maximo)
        }
}

data class DatosInicioInstructor(
    val aprendices: List<AprendizResumen>,
    val novedadesPendientes: Int?,
    val sensoresEnAlerta: Int?,
)

class InicioInstructorRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun cargar(): Resultado<DatosInicioInstructor> = try {
        coroutineScope {
            val resumen = async { http.get("aprendices/resumen") }
            val novedades = async { opcional { http.get("novedades") { parameter("estado", "pendiente") } } }
            val sensores = async { opcional { http.get("sensores") } }

            val respuesta = resumen.await()
            if (!respuesta.status.isSuccess()) {
                Resultado.Falla(
                    if (respuesta.status == HttpStatusCode.Forbidden) "No tienes permiso para ver el resumen de tus aprendices."
                    else "No se pudo cargar el resumen de tus aprendices."
                )
            } else {
                Resultado.Ok(
                    DatosInicioInstructor(
                        aprendices = respuesta.body(),
                        novedadesPendientes = novedades.await()?.let { r -> runCatching { r.body<JsonArray>().size }.getOrNull() },
                        sensoresEnAlerta = sensores.await()?.let { r -> runCatching { r.body<List<SensorEstado>>().count { it.enAlerta } }.getOrNull() },
                    )
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }

    private suspend fun opcional(bloque: suspend () -> HttpResponse): HttpResponse? = try {
        bloque().takeIf { it.status.isSuccess() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
