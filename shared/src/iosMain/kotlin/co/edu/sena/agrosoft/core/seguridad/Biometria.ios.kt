@file:OptIn(ExperimentalForeignApi::class, ExperimentalSettingsImplementation::class)

package co.edu.sena.agrosoft.core.seguridad

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LABiometryTypeFaceID
import platform.LocalAuthentication.LABiometryTypeTouchID
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

private val llavero by lazy { KeychainSettings(service = "co.edu.sena.agrosoft.credenciales") }

actual fun tipoBiometria(): TipoBiometria? {
    val contexto = LAContext()
    if (!contexto.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) return null
    return when (contexto.biometryType) {
        LABiometryTypeFaceID -> TipoBiometria.FACE_ID
        LABiometryTypeTouchID -> TipoBiometria.TOUCH_ID
        else -> null
    }
}

actual suspend fun autenticarBiometria(motivo: String): Boolean =
    suspendCancellableCoroutine { continuacion ->
        val contexto = LAContext()
        contexto.localizedCancelTitle = "Cancelar"
        contexto.evaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, motivo) { exito, _ ->
            if (continuacion.isActive) continuacion.resume(exito)
        }
    }

actual fun guardarCredenciales(credenciales: Credenciales) {
    llavero.putString("correo", credenciales.correo)
    llavero.putString("password", credenciales.password)
}

actual fun leerCredenciales(): Credenciales? {
    val correo = llavero.getStringOrNull("correo") ?: return null
    val password = llavero.getStringOrNull("password") ?: return null
    return Credenciales(correo, password)
}

actual fun borrarCredenciales() {
    llavero.remove("correo")
    llavero.remove("password")
}
