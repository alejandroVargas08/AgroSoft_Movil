package co.edu.sena.agrosoft.features.tareas.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import co.edu.sena.agrosoft.features.tareas.data.Tarea
import co.edu.sena.agrosoft.features.tareas.data.TareasRepositorio
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
}

class TareasViewModel(
    private val repositorio: TareasRepositorio = TareasRepositorio(),
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
        }
    }

    fun cambiarFiltro(filtro: FiltroTareas) {
        estado = estado.copy(filtro = filtro, abierta = null, entregando = null, aviso = null, errorAccion = null)
    }

    fun alternar(id: Int) {
        estado = estado.copy(
            abierta = if (estado.abierta == id) null else id,
            entregando = null,
            aviso = null,
            errorAccion = null,
        )
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
