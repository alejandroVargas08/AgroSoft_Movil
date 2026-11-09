package co.edu.sena.agrosoft.features.mas.data

import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.inicio.data.SensorEstado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlin.coroutines.cancellation.CancellationException

data class ResumenMas(
    val sensores: Int?,
    val sensoresEnAlerta: Int?,
    val novedadesPendientes: Int?,
)

class MasRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun resumen(rol: Rol): ResumenMas = coroutineScope {
        val sensores = async { seguro { http.get("sensores").takeIf { it.status.isSuccess() }?.body<List<SensorEstado>>() } }
        val novedades = async {
            val ruta = when (rol) {
                Rol.APRENDIZ -> "mis-novedades"
                Rol.INSTRUCTOR -> "novedades"
                Rol.ADMINISTRADOR -> null
            }
            ruta?.let { r ->
                seguro {
                    http.get(r) { parameter("estado", "pendiente") }
                        .takeIf { it.status.isSuccess() }
                        ?.body<JsonArray>()?.size
                }
            }
        }
        val lista = sensores.await()
        ResumenMas(
            sensores = lista?.count { it.activo },
            sensoresEnAlerta = lista?.count { it.enAlerta },
            novedadesPendientes = novedades.await(),
        )
    }

    private suspend fun <T> seguro(bloque: suspend () -> T?): T? = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
