package co.edu.sena.agrosoft.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.text.get

class AgroSoftMensajeria : FirebaseMessagingService() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        alcance.launch { RegistroPush.registrarToken(token) }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val titulo = mensaje.notification?.title ?: mensaje.data["titulo"] ?: "AgroSoft"
        val cuerpo = mensaje.notification?.body ?: mensaje.data["mensaje"] ?: return
        mostrarNotificacion(this, titulo, cuerpo)
        RegistroPush.avisarLlegada()
    }

    override fun onDestroy() {
        alcance.cancel()
        super.onDestroy()
    }
}

