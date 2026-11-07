package co.edu.sena.agrosoft.features.tareas.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.tareas.data.Evidencia
import co.edu.sena.agrosoft.features.tareas.data.EvidenciasRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import co.edu.sena.agrosoft.features.tareas.data.Tarea
import co.edu.sena.agrosoft.features.tareas.data.TareasRepositorio
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.compressImage
import kotlinx.coroutines.launch

enum class FiltroTareas(val etiqueta: String) {
    PENDIENTES("Pendientes"),
    ENTREGADAS("Entregadas"),
    APROBADAS("Aprobadas"),
}

private val ESTADOS_PENDIENTES = setOf("ASIGNADA", "EN_PROGRESO", "DEVUELTA")

data class TareasEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val tareas: List<Tarea> = emptyList(),
    val filtro: FiltroTareas = FiltroTareas.PENDIENTES,
    val abierta: Int? = null,
    val entregando: Int? = null,
    val horas: String = "",
    val comentario: String = "",
    val procesando: Boolean = false,
    val aviso: String? = null,
    val errorAccion: String? = null,
    val evidencias: Map<Int, List<Evidencia>> = emptyMap(),
    val fotosLocales: Map<Int, List<ByteArray>> = emptyMap(),
    val subiendoFoto: Boolean = false,
    val errorFoto: String? = null,
) {
    fun de(filtro: FiltroTareas): List<Tarea> = when (filtro) {
        FiltroTareas.PENDIENTES -> tareas
            .filter { it.estado in ESTADOS_PENDIENTES }
            .sortedWith(
                compareByDescending<Tarea> { it.estado == "DEVUELTA" }
                    .thenBy(nullsLast()) { Fechas.instante(it.fechaLimite)?.toEpochMilliseconds() }
            )
        FiltroTareas.ENTREGADAS -> tareas
            .filter { it.estado == "ENTREGADA" }
            .sortedByDescending { Fechas.instante(it.entregadaEn)?.toEpochMilliseconds() ?: 0L }
        FiltroTareas.APROBADAS -> tareas
            .filter { it.estado == "APROBADA" }
            .sortedByDescending { Fechas.instante(it.revisadaEn)?.toEpochMilliseconds() ?: 0L }
    }

    fun fotosGuardadas(actividadId: Int?): Int =
        actividadId?.let { id -> evidencias[id]?.sumOf { it.imagenes.size } } ?: 0
}

class TareasViewModel(
    private val repositorio: TareasRepositorio = TareasRepositorio(),
    private val evidenciasRepositorio: EvidenciasRepositorio = EvidenciasRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(TareasEstado())
        private set

    fun cargar() {
        estado = estado.copy(cargando = true, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.listar()) {
                is Resultado.Ok -> estado.copy(cargando = false, tareas = r.dato)
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
            estado.abierta?.let { id -> actividadDe(id)?.let(::cargarEvidencias) }
        }
    }

    fun cambiarFiltro(filtro: FiltroTareas) {
        estado = estado.copy(filtro = filtro, abierta = null, entregando = null, aviso = null, errorAccion = null, errorFoto = null)
    }

    fun alternar(id: Int) {
        val abriendo = estado.abierta != id
        estado = estado.copy(
            abierta = if (abriendo) id else null,
            entregando = null,
            aviso = null,
            errorAccion = null,
            errorFoto = null,
        )
        if (abriendo) actividadDe(id)?.let(::cargarEvidencias)
    }

    fun iniciar(id: Int) = ejecutar("Tarea iniciada.") { repositorio.iniciar(id) }

    fun empezarEntrega(id: Int) {
        estado = estado.copy(entregando = id, horas = "", comentario = "", errorAccion = null, aviso = null)
    }

    fun cancelarEntrega() {
        estado = estado.copy(entregando = null, errorAccion = null)
    }

    fun cambiarHoras(valor: String) {
        estado = estado.copy(
            horas = valor.filter { it.isDigit() || it == ',' || it == '.' }.take(5),
            errorAccion = null,
        )
    }

    fun cambiarComentario(valor: String) {
        estado = estado.copy(comentario = valor.take(1000))
    }

    fun entregar(id: Int) {
        val horas = estado.horas.replace(',', '.').toDoubleOrNull()
        if (horas == null || horas < 0.1) {
            estado = estado.copy(errorAccion = "Escribe las horas que trabajaste (mínimo 0,1).")
            return
        }
        val comentario = estado.comentario.trim()
        ejecutar("Tarea entregada. Tu instructor la revisará.") { repositorio.entregar(id, horas, comentario) }
    }

    fun subirFoto(actividadId: Int, original: ByteArray) {
        if (estado.subiendoFoto) return
        estado = estado.copy(subiendoFoto = true, errorFoto = null, aviso = null)
        viewModelScope.launch {
            val jpeg = runCatching {
                FileKit.compressImage(
                    bytes = original,
                    quality = 80,
                    maxWidth = 1600,
                    maxHeight = 1600,
                    imageFormat = ImageFormat.JPEG,
                )
            }.getOrDefault(original)

            estado = when (val r = evidenciasRepositorio.subirFoto(actividadId, jpeg)) {
                is Resultado.Ok -> estado.copy(
                    subiendoFoto = false,
                    aviso = "Foto guardada como evidencia.",
                    fotosLocales = estado.fotosLocales + (actividadId to estado.fotosLocales[actividadId].orEmpty() + jpeg),
                    evidencias = estado.evidencias + (actividadId to estado.evidencias[actividadId].orEmpty() + r.dato),
                )
                is Resultado.Falla -> estado.copy(subiendoFoto = false, errorFoto = r.mensaje)
            }
        }
    }

    fun avisarErrorFoto(mensaje: String) {
        estado = estado.copy(errorFoto = mensaje)
    }

    private fun actividadDe(tareaId: Int): Int? =
        estado.tareas.firstOrNull { it.id == tareaId }?.actividad?.id

    private fun cargarEvidencias(actividadId: Int) {
        viewModelScope.launch {
            val r = evidenciasRepositorio.listar(actividadId)
            if (r is Resultado.Ok) {
                estado = estado.copy(evidencias = estado.evidencias + (actividadId to r.dato))
            }
        }
    }

    private fun ejecutar(mensaje: String, llamada: suspend () -> Resultado<Tarea>) {
        if (estado.procesando) return
        estado = estado.copy(procesando = true, errorAccion = null)
        viewModelScope.launch {
            estado = when (val r = llamada()) {
                is Resultado.Ok -> {
                    val nueva = r.dato
                    estado.copy(
                        procesando = false,
                        entregando = null,
                        aviso = mensaje,
                        tareas = estado.tareas.map { vieja ->
                            if (vieja.id != nueva.id) vieja
                            else nueva.copy(
                                actividad = nueva.actividad ?: vieja.actividad,
                                asignadoPor = nueva.asignadoPor ?: vieja.asignadoPor,
                            )
                        },
                    )
                }
                is Resultado.Falla -> estado.copy(procesando = false, errorAccion = r.mensaje)
            }
        }
    }
}
