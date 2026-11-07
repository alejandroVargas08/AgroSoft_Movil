package co.edu.sena.agrosoft.core.session

import co.edu.sena.agrosoft.features.auth.data.Sesion
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

object AlmacenSesion {
    private val ajustes = Settings()
    private val json = Json { ignoreUnknownKeys = true }

    fun guardar(sesion: Sesion, token: String) {
        ajustes.putString("token", token)
        ajustes.putString("sesion", json.encodeToString(Sesion.serializer(), sesion))
    }

    fun token(): String? = ajustes.getStringOrNull("token")

    fun sesion(): Sesion? = ajustes.getStringOrNull("sesion")?.let {
        runCatching { json.decodeFromString(Sesion.serializer(), it) }.getOrNull()
    }

    fun borrar() {
        ajustes.remove("token")
        ajustes.remove("sesion")
    }
}
//Arreglar el este