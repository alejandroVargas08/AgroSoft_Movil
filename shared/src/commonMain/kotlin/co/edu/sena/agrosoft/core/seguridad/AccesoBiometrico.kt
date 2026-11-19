package co.edu.sena.agrosoft.core.seguridad

import com.russhwolf.settings.Settings

object AccesoBiometrico {

    private const val CLAVE_ACTIVA = "biometria_activa"

    private val ajustes: Settings by lazy { Settings() }

    val tipo: TipoBiometria?
        get() = runCatching { tipoBiometria() }.getOrNull()

    private fun guardadas(): Credenciales? = runCatching { leerCredenciales() }.getOrNull()

    private var bandera: Boolean
        get() = ajustes.getBooleanOrNull(CLAVE_ACTIVA) ?: (guardadas() != null)
        set(valor) = ajustes.putBoolean(CLAVE_ACTIVA, valor)

    val activo: Boolean
        get() = bandera && guardadas() != null

    var rechazado: Boolean
        get() = ajustes.getBoolean("biometria_rechazada", false)
        set(valor) = ajustes.putBoolean("biometria_rechazada", valor)

    fun recordar(credenciales: Credenciales) {
        bandera = bandera
        runCatching { guardarCredenciales(credenciales) }
    }

    fun recordadas(correo: String): Credenciales? =
        guardadas()?.takeIf { it.correo.equals(correo.trim(), ignoreCase = true) }

    fun activar(credenciales: Credenciales? = null) {
        if (credenciales != null) runCatching { guardarCredenciales(credenciales) }
        bandera = true
        rechazado = false
    }

    fun desactivar() {
        bandera = false
    }

    fun olvidarSiInactivo() {
        if (!activo) {
            bandera = false
            runCatching { borrarCredenciales() }
        }
    }

    suspend fun obtener(motivo: String): Credenciales? {
        if (!activo) return null
        return if (autenticarBiometria(motivo)) guardadas() else null
    }
}
