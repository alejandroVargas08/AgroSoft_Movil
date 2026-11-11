package co.edu.sena.agrosoft.core.seguridad

import com.russhwolf.settings.Settings

object AccesoBiometrico {

    private val ajustes: Settings by lazy { Settings() }

    val tipo: TipoBiometria?
        get() = runCatching { tipoBiometria() }.getOrNull()

    val activo: Boolean
        get() = runCatching { leerCredenciales() != null }.getOrDefault(false)

    var rechazado: Boolean
        get() = ajustes.getBoolean("biometria_rechazada", false)
        set(valor) = ajustes.putBoolean("biometria_rechazada", valor)

    fun activar(credenciales: Credenciales) {
        guardarCredenciales(credenciales)
        rechazado = false
    }

    fun desactivar() {
        runCatching { borrarCredenciales() }
    }

    suspend fun obtener(motivo: String): Credenciales? {
        if (!activo) return null
        return if (autenticarBiometria(motivo)) leerCredenciales() else null
    }
}
