package co.edu.sena.agrosoft.core.push

import androidx.compose.runtime.Composable

actual val plataformaPush: String = "ios"

actual suspend fun obtenerTokenPush(): String? = null

@Composable
actual fun PermisoNotificaciones() {
}
