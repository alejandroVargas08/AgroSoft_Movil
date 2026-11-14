package co.edu.sena.agrosoft.core.push

import androidx.compose.runtime.Composable

expect val plataformaPush: String

expect suspend fun obtenerTokenPush(): String?

@Composable
expect fun PermisoNotificaciones()
