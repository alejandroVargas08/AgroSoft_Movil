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

const val MAX_FOTOS = 5
private const val PREGUNTA_FOTO = "¿Qué ves en esta imagen? ¿Hay plagas, enfermedades o deficiencias?"

data class MensajeChat(
    val deUsuario: Boolean,
    val texto: String,
    val error: Boolean = false,
    val fotos: List<ByteArray> = emptyList(),
    val cantidadFotos: Int = 0,
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
    val analizandoFoto: Boolean = false,
    val texto: String = "",
    val adjuntos: List<ByteArray> = emptyList(),
    val preparandoFoto: Boolean = false,
    val ultimoTexto: String? = null,
    val ultimasFotos: List<ByteArray> = emptyList(),
    val error: String? = null,
) {
    val puedeEnviar: Boolean get() = !enviando && !preparandoFoto && (texto.isNotBlank() || adjuntos.isNotEmpty())
}

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
        estado = estado.copy(
            abierta = true,
            conversacionId = null,
            titulo = null,
            mensajes = emptyList(),
            texto = "",
            adjuntos = emptyList(),
        )
    }

    fun abrir(c: Conversacion) {
        estado = estado.copy(
            abierta = true,
            conversacionId = c.id,
            titulo = c.titulo,
            mensajes = emptyList(),
            cargandoMensajes = true,
            texto = "",
            adjuntos = emptyList(),
        )
        viewModelScope.launch {
            estado = when (val r = repositorio.mensajes(c.id)) {
                is Resultado.Ok -> estado.copy(
                    cargandoMensajes = false,
                    mensajes = r.dato.map {
                        MensajeChat(
                            deUsuario = it.rol == "user",
                            texto = it.contenido,
                            cantidadFotos = if (it.tieneImagenes) it.cantidadImagenes else 0,
                        )
                    },
                )
                is Resultado.Falla -> estado.copy(
                    cargandoMensajes = false,
                    mensajes = listOf(MensajeChat(false, r.mensaje, error = true)),
                )
            }
        }
    }

    fun cerrarChat() {
        estado = estado.copy(abierta = false, texto = "", adjuntos = emptyList())
        cargar()
    }

    fun cambiarTexto(v: String) {
        estado = estado.copy(texto = v)
    }

    fun preparandoFoto(activo: Boolean) {
        estado = estado.copy(preparandoFoto = activo)
    }

    fun adjuntar(foto: ByteArray) {
        if (!estado.abierta) nueva()
        if (estado.adjuntos.size >= MAX_FOTOS) return
        estado = estado.copy(adjuntos = estado.adjuntos + foto, preparandoFoto = false)
    }

    fun quitarAdjunto(indice: Int) {
        estado = estado.copy(adjuntos = estado.adjuntos.filterIndexed { i, _ -> i != indice })
    }

    fun enviar(sugerencia: String? = null) {
        val fotos = if (sugerencia == null) estado.adjuntos else emptyList()
        val escrito = (sugerencia ?: estado.texto).trim()
        if ((escrito.isEmpty() && fotos.isEmpty()) || estado.enviando) return
        if (!estado.abierta) nueva()
        val texto = escrito.ifEmpty { PREGUNTA_FOTO }

        estado = estado.copy(
            mensajes = estado.mensajes + MensajeChat(true, texto, fotos = fotos, cantidadFotos = fotos.size),
            texto = "",
            adjuntos = emptyList(),
            enviando = true,
            analizandoFoto = fotos.isNotEmpty(),
            ultimoTexto = texto,
            ultimasFotos = fotos,
        )

        viewModelScope.launch {
            val titulo = if (fotos.isNotEmpty() && escrito.isEmpty()) "Análisis de foto" else texto.take(48)
            val id = estado.conversacionId ?: when (val c = repositorio.crear(titulo)) {
                is Resultado.Ok -> {
                    estado = estado.copy(conversacionId = c.dato.id, titulo = c.dato.titulo)
                    c.dato.id
                }
                is Resultado.Falla -> {
                    estado = estado.copy(
                        enviando = false,
                        analizandoFoto = false,
                        mensajes = estado.mensajes + MensajeChat(false, c.mensaje, error = true),
                    )
                    return@launch
                }
            }

            val r = if (fotos.isNotEmpty()) repositorio.analizarImagenes(id, fotos, texto) else repositorio.enviar(id, texto)
            estado = when (r) {
                is Resultado.Ok -> estado.copy(
                    enviando = false,
                    analizandoFoto = false,
                    mensajes = estado.mensajes + MensajeChat(false, r.dato.respuesta),
                )
                is Resultado.Falla -> estado.copy(
                    enviando = false,
                    analizandoFoto = false,
                    mensajes = estado.mensajes + MensajeChat(false, r.mensaje, error = true),
                )
            }
        }
    }

    fun reintentar() {
        val texto = estado.ultimoTexto ?: return
        val fotos = estado.ultimasFotos
        estado = estado.copy(
            mensajes = estado.mensajes.dropLastWhile { it.error }.dropLast(1),
            adjuntos = fotos,
            texto = texto,
        )
        enviar()
    }

    fun eliminar(c: Conversacion) {
        estado = estado.copy(conversaciones = estado.conversaciones.filterNot { it.id == c.id })
        viewModelScope.launch { repositorio.eliminar(c.id) }
    }
}
