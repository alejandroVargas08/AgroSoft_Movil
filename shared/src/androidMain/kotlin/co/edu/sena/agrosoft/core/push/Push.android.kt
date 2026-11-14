package co.edu.sena.agrosoft.core.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.compareTo
import kotlin.coroutines.resume
import kotlin.text.toInt

const val CANAL_AVISOS = "agrosoft_avisos"

actual val plataformaPush: String = "android"

@Suppress("DEPRECATION")
actual suspend fun obtenerTokenPush(): String? = suspendCancellableCoroutine { continuacion ->
    try {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
            if (continuacion.isActive) continuacion.resume(if (tarea.isSuccessful) tarea.result else null)
        }
    } catch (_: Exception) {
        if (continuacion.isActive) continuacion.resume(null)
    }
}

@Composable
actual fun PermisoNotificaciones() {
    val contexto = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        crearCanalAvisos(contexto)
        if (Build.VERSION.SDK_INT >= 33 && !tienePermiso(contexto)) {
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

fun crearCanalAvisos(contexto: Context) {
    if (Build.VERSION.SDK_INT < 26) return
    val gestor = contexto.getSystemService(NotificationManager::class.java) ?: return
    if (gestor.getNotificationChannel(CANAL_AVISOS) != null) return
    gestor.createNotificationChannel(
        NotificationChannel(CANAL_AVISOS, "Avisos de AgroSoft", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Tareas, solicitudes y alertas"
        }
    )
}

private fun tienePermiso(contexto: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission", "DiscouragedApi")
fun mostrarNotificacion(contexto: Context, titulo: String, cuerpo: String) {
    crearCanalAvisos(contexto)
    if (!tienePermiso(contexto)) return

    val abrir = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendiente = abrir?.let {
        PendingIntent.getActivity(contexto, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
    val icono = contexto.resources.getIdentifier("ic_notificacion", "drawable", contexto.packageName)
        .takeIf { it != 0 } ?: contexto.applicationInfo.icon

    val notificacion = NotificationCompat.Builder(contexto, CANAL_AVISOS)
        .setSmallIcon(icono)
        .setColor(0xFF3B7D48.toInt())
        .setContentTitle(titulo)
        .setContentText(cuerpo)
        .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendiente)
        .build()

    NotificationManagerCompat.from(contexto).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notificacion)
}

