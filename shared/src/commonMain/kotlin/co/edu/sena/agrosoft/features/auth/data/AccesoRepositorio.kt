package co.edu.sena.agrosoft.features.auth.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

private const val TIPO_RESTABLECER = "restablecer_contrasena"

@Serializable
data class FichaPublica(
    val id: Int,
    val nombre: String = "",
    val tipo: String? = null,
    val ficha: JsonElement? = null,
) {
    val numero: String get() = (ficha as? JsonPrimitive)?.contentOrNull.orEmpty()
}

@Serializable
data class SolicitudRegistroDto(
    val nombre: String,
    val apellido: String,
    val identificacion: String,
    val telefono: String,
    val email: String,
    val password: String,
    val tipoUsuario: String,
    val programaFormacionId: Int? = null,
    val codigoInvitacion: String? = null,
    val codigoInstructor: String? = null,
    val fichasACargo: List<String>? = null,
    val cargo: String? = null,
    val motivoSolicitud: String? = null,
)

@Serializable
private data class EnviarCodigoDto(val email: String, val tipo: String)

@Serializable
private data class VerificarCodigoDto(val email: String, val codigo: String, val tipo: String)

@Serializable
private data class RestablecerDto(val email: String, val codigo: String, val nuevaContrasena: String)

class AccesoRepositorio {

    private val http = ApiCliente.http
    private val lento = ApiCliente.subidas

    suspend fun enviarCodigo(correo: String): Resultado<Unit> =
        accion {
            lento.post("email-codes/enviar") {
                contentType(ContentType.Application.Json)
                setBody(EnviarCodigoDto(correo, TIPO_RESTABLECER))
            }
        }

    suspend fun verificarCodigo(correo: String, codigo: String): Resultado<Unit> =
        accion { http.post("email-codes/verificar") { setBody(VerificarCodigoDto(correo, codigo, TIPO_RESTABLECER)) } }

    suspend fun restablecer(correo: String, codigo: String, nueva: String): Resultado<Unit> =
        accion { http.post("email-codes/restablecer") { setBody(RestablecerDto(correo, codigo, nueva)) } }

    suspend fun fichas(): Resultado<List<FichaPublica>> = try {
        val r = http.get("programas-formacion")
        if (r.status.isSuccess()) Resultado.Ok(r.body()) else Resultado.Falla(mensajeDe(r))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudieron cargar las fichas.")
    }

    suspend fun registrar(dto: SolicitudRegistroDto): Resultado<Boolean> = try {
        val r = lento.post("usuarios") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        if (r.status.isSuccess()) {
            val cuerpo = runCatching { r.body<JsonObject>() }.getOrNull()
            Resultado.Ok(cuerpo?.get("requiereAprobacion")?.jsonPrimitive?.booleanOrNull ?: false)
        } else {
            Resultado.Falla(mensajeDe(r))
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
        Resultado.Falla("El servidor tardó demasiado en responder. Revisa tu correo: puede que el código sí haya llegado.")
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
