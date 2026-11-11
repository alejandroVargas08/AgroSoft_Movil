package co.edu.sena.agrosoft.core.seguridad

import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

object PuenteAndroid {
    private var referencia: WeakReference<FragmentActivity>? = null
    val actividad: FragmentActivity? get() = referencia?.get()

    fun registrar(actividad: FragmentActivity) {
        referencia = WeakReference(actividad)
    }
}

private const val AUTENTICADORES = BiometricManager.Authenticators.BIOMETRIC_STRONG

private var preferenciasCache: SharedPreferences? = null

private fun preferencias(): SharedPreferences? {
    preferenciasCache?.let { return it }
    val contexto = PuenteAndroid.actividad?.applicationContext ?: return null
    val llave = MasterKey.Builder(contexto).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    return EncryptedSharedPreferences.create(
        contexto,
        "agrosoft_credenciales",
        llave,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    ).also { preferenciasCache = it }
}

actual fun tipoBiometria(): TipoBiometria? {
    val actividad = PuenteAndroid.actividad ?: return null
    val estado = BiometricManager.from(actividad).canAuthenticate(AUTENTICADORES)
    return if (estado == BiometricManager.BIOMETRIC_SUCCESS) TipoBiometria.HUELLA else null
}

actual suspend fun autenticarBiometria(motivo: String): Boolean {
    val actividad = PuenteAndroid.actividad ?: return false
    return withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuacion ->
            val aviso = BiometricPrompt(
                actividad,
                ContextCompat.getMainExecutor(actividad),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuacion.isActive) continuacion.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuacion.isActive) continuacion.resume(false)
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("AgroSoft")
                .setSubtitle(motivo)
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(AUTENTICADORES)
                .build()
            aviso.authenticate(info)
        }
    }
}

actual fun guardarCredenciales(credenciales: Credenciales) {
    preferencias()?.edit()
        ?.putString("correo", credenciales.correo)
        ?.putString("password", credenciales.password)
        ?.apply()
}

actual fun leerCredenciales(): Credenciales? {
    val p = preferencias() ?: return null
    val correo = p.getString("correo", null) ?: return null
    val password = p.getString("password", null) ?: return null
    return Credenciales(correo, password)
}

actual fun borrarCredenciales() {
    preferencias()?.edit()?.clear()?.apply()
}
