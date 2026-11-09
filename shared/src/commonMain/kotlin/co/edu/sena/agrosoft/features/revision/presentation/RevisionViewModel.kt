package co.edu.sena.agrosoft.features.revision.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.revision.data.Entrega
import co.edu.sena.agrosoft.features.revision.data.RevisionRepositorio
import co.edu.sena.agrosoft.features.tareas.data.EvidenciasRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.launch

enum class Decision { APROBAR, DEVOLVER }

data class RevisionEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val entregas: List<Entrega> = emptyList(),
    val abierta: Int? = null,
    val decision: Decision? = null,
    val comentario: String = "",
    val procesando: Boolean = false,
    val aviso: String? = null,
    val errorAccion: String? = null,
    val fotos: Map<Int, List<String>> = emptyMap(),
)

class RevisionViewModel(
    private val repositorio: RevisionRepositorio = RevisionRepositorio(),
    private val evidencias: EvidenciasRepositorio = EvidenciasRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(RevisionEstado())
        private set

    fun cargar() {
        estado = estado.copy(cargando = true, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.porRevisar()) {
                is Resultado.Ok -> estado.copy(
                    cargando = false,
                    entregas = r.dato.sortedBy { Fechas.instante(it.entregadaEn)?.toEpochMilliseconds() ?: 0L },
                )
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun alternar(id: Int) {
        val abriendo = estado.abierta != id
        estado = estado.copy(
            abierta = if (abriendo) id else null,
            decision = null,
            comentario = "",
            aviso = null,
            errorAccion = null,
        )
        if (abriendo) {
            estado.entregas.firstOrNull { it.id == id }?.actividad?.id?.let(::cargarFotos)
        }
    }

    fun elegir(decision: Decision) {
        estado = estado.copy(decision = decision, comentario = "", errorAccion = null)
    }

    fun cancelar() {
        estado = estado.copy(decision = null, comentario = "", errorAccion = null)
    }

    fun cambiarComentario(valor: String) {
        estado = estado.copy(comentario = valor.take(1000), errorAccion = null)
    }

    fun confirmar(entrega: Entrega) {
        val decision = estado.decision ?: return
        if (estado.procesando) return
        val comentario = estado.comentario.trim()
        if (decision == Decision.DEVOLVER && comentario.length < 3) {
            estado = estado.copy(errorAccion = "Escribe qué debe corregir el aprendiz (mínimo 3 caracteres).")
            return
        }
        estado = estado.copy(procesando = true, errorAccion = null)
        viewModelScope.launch {
            val r = when (decision) {
                Decision.APROBAR -> repositorio.aprobar(entrega.id, comentario)
                Decision.DEVOLVER -> repositorio.devolver(entrega.id, comentario)
            }
            val nombre = entrega.aprendiz?.nombre ?: "el aprendiz"
            estado = when (r) {
                is Resultado.Ok -> estado.copy(
                    procesando = false,
                    entregas = estado.entregas.filterNot { it.id == entrega.id },
                    abierta = null,
                    decision = null,
                    comentario = "",
                    aviso = if (decision == Decision.APROBAR) "Tarea de $nombre aprobada." else "Tarea devuelta a $nombre.",
                )
                is Resultado.Falla -> estado.copy(procesando = false, errorAccion = r.mensaje)
            }
        }
    }

    private fun cargarFotos(actividadId: Int) {
        viewModelScope.launch {
            val r = evidencias.listar(actividadId)
            if (r is Resultado.Ok) {
                estado = estado.copy(fotos = estado.fotos + (actividadId to r.dato.flatMap { it.imagenes }))
            }
        }
    }
}
