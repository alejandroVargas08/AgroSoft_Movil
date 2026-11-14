package co.edu.sena.agrosoft.core.push

import co.edu.sena.agrosoft.core.network.ApiCliente
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

@Serializable
private data class DispositivoPush(val token: String, val plataforma: String)

@Serializable
private data class BajaPush(val token: String)

object RegistroPush {

    private var registrado: String? = null
    private val _llegadas = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val llegadas: SharedFlow<Unit> = _llegadas

    fun avisarLlegada() {
        _llegadas.tryEmit(Unit)
    }

    suspend fun registrar() {
        val token = obtenerTokenPush() ?: return
        registrarToken(token)
    }

    suspend fun registrarToken(token: String) {
        if (ApiCliente.token == null) return
        try {
            val respuesta = ApiCliente.http.post("push/dispositivos") {
                setBody(DispositivoPush(token, plataformaPush))
            }
            if (respuesta.status.isSuccess()) registrado = token
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun darDeBaja() {
        val token = registrado ?: return
        registrado = null
        try {
            ApiCliente.http.post("push/dispositivos/baja") { setBody(BajaPush(token)) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }
}
