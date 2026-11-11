package co.edu.sena.agrosoft.features.perfil.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class ProgramaPerfil(
    val nombre: String? = null,
    val tipo: String? = null,
    val ficha: JsonElement? = null,
) {
    val numeroFicha: String? get() = (ficha as? JsonPrimitive)?.contentOrNull
}

@Serializable
data class Perfil(
    val id: Int,
    val nombre: String,
    val apellido: String = "",
    val identificacion: JsonElement? = null,
    val correo: String? = null,
    val telefono: String? = null,
    val estado: String? = null,
    val correoVerificado: Boolean = false,
    val dosPasosActivo: Boolean = false,
    val rol: String? = null,
    val programaFormacion: ProgramaPerfil? = null,
) {
    val nombreCompleto: String get() = "$nombre $apellido".trim()
    val documento: String? get() = (identificacion as? JsonPrimitive)?.contentOrNull
}

@Serializable
data class CambiosPerfil(
    val nombre: String? = null,
    val apellido: String? = null,
    val telefono: String? = null,
    val dosPasosActivo: Boolean? = null,
)

@Serializable
private data class LoginDto(val correo: String, val password: String)

class PerfilRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun obtener(id: Int): Resultado<Perfil> = try {
        val r = http.get("usuarios/$id")
        if (r.status.isSuccess()) Resultado.Ok(r.body()) else Resultado.Falla(mensajeDe(r))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }

    suspend fun actualizar(id: Int, cambios: CambiosPerfil): Resultado<Unit> = try {
        val r = http.patch("usuarios/$id") { setBody(cambios) }
        if (r.status.isSuccess()) Resultado.Ok(Unit) else Resultado.Falla(mensajeDe(r))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor.")
    }

    suspend fun verificarContrasena(correo: String, password: String): Resultado<Unit> = try {
        val r = http.post("usuarios/login") { setBody(LoginDto(correo, password)) }
        when {
            r.status.isSuccess() -> Resultado.Ok(Unit)
            r.status == HttpStatusCode.Unauthorized || r.status == HttpStatusCode.BadRequest ->
                Resultado.Falla("La contraseña no es correcta.")
            else -> Resultado.Falla(mensajeDe(r))
        }
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
