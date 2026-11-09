package co.edu.sena.agrosoft.features.agrobot.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.agrobot.data.AgroBotRepositorio
import co.edu.sena.agrosoft.features.agrobot.data.Conversacion
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.launch

data class MensajeChat(
    val deUsuario: Boolean,
    val texto: String,
    val error: Boolean = false,
)

data class AgroBotEstado(
    val cargando: Boolean = false,
    val conversaciones: List<Conversacion> = emptyList(),
    val abierta: Boolean = false,
    val conversacionId: Int? = null,
    val titulo: String? = null,
    val mensajes: List<MensajeChat> = emptyList(),
    val cargandoMensajes: Boolean = false,
    val enviando: Boolean = false,
    val texto: String = "",
    val ultimoEnviado: String? = null,
    val error: String? = null,
)

class AgroBotViewModel(
    private val usuarioId: Int,
    private val repositorio: AgroBotRepositorio = AgroBotRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(AgroBotEstado())
        private set

    fun cargar() {
        estado = estado.copy(cargando = estado.conversaciones.isEmpty(), error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.conversaciones(usuarioId)) {
                is Resultado.Ok -> estado.copy(
                    cargando = false,
                    conversaciones = r.dato.sortedByDescending { it.actualizadoEn ?: it.creadoEn.orEmpty() },
                )
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun nueva() {
        estado = estado.copy(abierta = true, conversacionId = null, titulo = null, mensajes = emptyList(), texto = "")
    }

    fun abrir(c: Conversacion) {
        estado = estado.copy(
            abierta = true,
            conversacionId = c.id,
            titulo = c.titulo,
            mensajes = emptyList(),
            cargandoMensajes = true,
            texto = "",
        )
        viewModelScope.launch {
            estado = when (val r = repositorio.mensajes(c.id)) {
                is Resultado.Ok -> estado.copy(
                    cargandoMensajes = false,
                    mensajes = r.dato.map { MensajeChat(deUsuario = it.rol == "user", texto = it.contenido) },
                )
                is Resultado.Falla -> estado.copy(
                    cargandoMensajes = false,
                    mensajes = listOf(MensajeChat(false, r.mensaje, error = true)),
                )
            }
        }
    }

    fun cerrarChat() {
        estado = estado.copy(abierta = false, texto = "")
        cargar()
    }

    fun cambiarTexto(v: String) {
        estado = estado.copy(texto = v)
    }

    fun enviar(sugerencia: String? = null) {
        val texto = (sugerencia ?: estado.texto).trim()
        if (texto.isEmpty() || estado.enviando) return
        if (!estado.abierta) nueva()
        estado = estado.copy(
            mensajes = estado.mensajes + MensajeChat(true, texto),
            texto = "",
            enviando = true,
            ultimoEnviado = texto,
        )
        viewModelScope.launch {
            val id = estado.conversacionId ?: when (val c = repositorio.crear(texto.take(48))) {
                is Resultado.Ok -> {
                    estado = estado.copy(conversacionId = c.dato.id, titulo = c.dato.titulo)
                    c.dato.id
                }
                is Resultado.Falla -> {
                    estado = estado.copy(enviando = false, mensajes = estado.mensajes + MensajeChat(false, c.mensaje, error = true))
                    return@launch
                }
            }
            estado = when (val r = repositorio.enviar(id, texto)) {
                is Resultado.Ok -> estado.copy(enviando = false, mensajes = estado.mensajes + MensajeChat(false, r.dato.respuesta))
                is Resultado.Falla -> estado.copy(enviando = false, mensajes = estado.mensajes + MensajeChat(false, r.mensaje, error = true))
            }
        }
    }

    fun reintentar() {
        val ultimo = estado.ultimoEnviado ?: return
        estado = estado.copy(mensajes = estado.mensajes.dropLastWhile { it.error }.dropLast(1))
        enviar(ultimo)
    }

    fun eliminar(c: Conversacion) {
        estado = estado.copy(conversaciones = estado.conversaciones.filterNot { it.id == c.id })
        viewModelScope.launch { repositorio.eliminar(c.id) }
    }
}
