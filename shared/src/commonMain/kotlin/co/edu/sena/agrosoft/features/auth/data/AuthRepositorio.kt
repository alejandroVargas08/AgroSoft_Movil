package co.edu.sena.agrosoft.features.auth.data

import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.core.session.AlmacenSesion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class Sesion(
    val id: Int,
    val nombre: String,
    val nombreCompleto: String,
    val iniciales: String,
    val correo: String,
    val rol: Rol,
)

@Serializable
private data class LoginPeticion(val correo: String, val password: String)

@Serializable
private data class UsuarioLogin(val id: Int, val nombre: String, val correo: String, val rolId: Int)

@Serializable
private data class LoginRespuesta(
    @SerialName("access_token") val accessToken: String,
    val usuario: UsuarioLogin,
)

@Serializable
private data class MiAcceso(val id: Int, val rolId: Int, val permisos: List<String> = emptyList())

sealed interface ResultadoLogin {
    data class Exito(val sesion: Sesion) : ResultadoLogin
    data class AvisoCuenta(val mensaje: String) : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

class AuthRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun iniciarSesion(correo: String, password: String): ResultadoLogin {
        return try {
            val respuesta = http.post("usuarios/login") {
                setBody(LoginPeticion(correo.trim(), password))
            }
            if (!respuesta.status.isSuccess()) {
                val (mensaje, estado) = leerError(respuesta)
                return if (estado != null) ResultadoLogin.AvisoCuenta(mensaje) else ResultadoLogin.Error(mensaje)
            }

            val datos = respuesta.body<LoginRespuesta>()
            ApiCliente.token = datos.accessToken

            val accesoRespuesta = http.get("mi-acceso")
            if (!accesoRespuesta.status.isSuccess()) {
                ApiCliente.token = null
                return ResultadoLogin.Error(leerError(accesoRespuesta).first)
            }
            val acceso = accesoRespuesta.body<MiAcceso>()

            val sesion = Sesion(
                id = datos.usuario.id,
                nombre = datos.usuario.nombre.trim().substringBefore(" "),
                nombreCompleto = datos.usuario.nombre.trim(),
                iniciales = inicialesDe(datos.usuario.nombre),
                correo = datos.usuario.correo,
                rol = rolDesdePermisos(acceso.permisos),
            )
            AlmacenSesion.guardar(sesion, datos.accessToken)
            ResultadoLogin.Exito(sesion)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiCliente.token = null
            ResultadoLogin.Error("No se pudo conectar con el servidor. Revisa tu conexión.")
        }
    }

    suspend fun restaurarSesion(): Sesion? {
        val token = AlmacenSesion.token() ?: return null
        val guardada = AlmacenSesion.sesion() ?: return null
        ApiCliente.token = token

        return try {
            val respuesta = http.get("mi-acceso")
            when {
                respuesta.status == HttpStatusCode.Unauthorized -> {
                    cerrarSesion()
                    null
                }
                respuesta.status.isSuccess() -> {
                    val acceso = respuesta.body<MiAcceso>()
                    val actualizada = guardada.copy(rol = rolDesdePermisos(acceso.permisos))
                    AlmacenSesion.guardar(actualizada, token)
                    actualizada
                }
                else -> guardada
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            guardada
        }
    }

    fun cerrarSesion() {
        ApiCliente.token = null
        AlmacenSesion.borrar()
    }

    private suspend fun leerError(respuesta: HttpResponse): Pair<String, String?> {
        val cuerpo = runCatching { respuesta.body<JsonObject>() }.getOrNull()
        val mensaje = when (val m = cuerpo?.get("message")) {
            is JsonArray -> m.firstOrNull()?.jsonPrimitive?.contentOrNull
            null -> null
            else -> m.jsonPrimitive.contentOrNull
        } ?: "Error al iniciar sesión"
        val estado = cuerpo?.get("estado")?.jsonPrimitive?.contentOrNull
        return mensaje to estado
    }

    private fun rolDesdePermisos(permisos: List<String>): Rol = when {
        "usuarios.gestionar" in permisos -> Rol.ADMINISTRADOR
        "actividades.asignar" in permisos -> Rol.INSTRUCTOR
        else -> Rol.APRENDIZ
    }

    private fun inicialesDe(nombre: String): String {
        val partes = nombre.trim().split(" ").filter { it.isNotBlank() }
        val segunda = when {
            partes.size >= 4 -> partes[2]
            partes.size >= 2 -> partes[1]
            else -> null
        }
        return (partes.firstOrNull()?.take(1).orEmpty() + segunda?.take(1).orEmpty()).uppercase()
    }
}
//Paso al loginviewModel