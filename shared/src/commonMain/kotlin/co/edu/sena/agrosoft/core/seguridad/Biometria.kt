package co.edu.sena.agrosoft.core.seguridad

enum class TipoBiometria(val nombre: String) {
    FACE_ID("Face ID"),
    TOUCH_ID("Touch ID"),
    HUELLA("huella"),
}

data class Credenciales(val correo: String, val password: String)

expect fun tipoBiometria(): TipoBiometria?

expect suspend fun autenticarBiometria(motivo: String): Boolean

expect fun guardarCredenciales(credenciales: Credenciales)

expect fun leerCredenciales(): Credenciales?

expect fun borrarCredenciales()
