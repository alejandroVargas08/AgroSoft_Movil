package co.edu.sena.agrosoft.features.notificaciones.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.notificaciones.data.Notificacion
import co.edu.sena.agrosoft.features.notificaciones.data.NotificacionesRepositorio
import co.edu.sena.agrosoft.features.revision.data.RevisionRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import co.edu.sena.agrosoft.features.tareas.data.TareasRepositorio
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class AvisosEstado(
    val cargando: Boolean = false,
    val error: String? = null,
    val notificaciones: List<Notificacion> = emptyList(),
    val pendientesTrabajo: Int = 0,
) {
    val noLeidas: Int get() = notificaciones.count { !it.leida }
}

class NotificacionesViewModel(
    private val usuarioId: Int,
    private val rol: Rol,
    private val repositorio: NotificacionesRepositorio = NotificacionesRepositorio(),
    private val tareas: TareasRepositorio = TareasRepositorio(),
    private val revision: RevisionRepositorio = RevisionRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(AvisosEstado())
        private set

    private var refrescando = false

    fun refrescar() {
        if (refrescando) return
        refrescando = true
        estado = estado.copy(cargando = estado.notificaciones.isEmpty(), error = null)
        viewModelScope.launch {
            try {
                val (notificaciones, pendientes) = coroutineScope {
                    val n = async { repositorio.listar(usuarioId) }
                    val p = async { contarPendientes() }
                    n.await() to p.await()
                }
                estado = when (notificaciones) {
                    is Resultado.Ok -> estado.copy(
                        cargando = false,
                        notificaciones = notificaciones.dato.sortedByDescending {
                            Fechas.instante(it.creadoEn)?.toEpochMilliseconds() ?: 0L
                        },
                        pendientesTrabajo = pendientes ?: estado.pendientesTrabajo,
                    )
                    is Resultado.Falla -> estado.copy(
                        cargando = false,
                        error = notificaciones.mensaje,
                        pendientesTrabajo = pendientes ?: estado.pendientesTrabajo,
                    )
                }
            } finally {
                refrescando = false
            }
        }
    }

    fun marcarLeida(id: Int) {
        if (estado.notificaciones.none { it.id == id && !it.leida }) return
        estado = estado.copy(notificaciones = estado.notificaciones.map { if (it.id == id) it.copy(leida = true) else it })
        viewModelScope.launch { repositorio.marcarLeida(id) }
    }

    fun marcarTodas() {
        val ids = estado.notificaciones.filter { !it.leida }.map { it.id }
        if (ids.isEmpty()) return
        estado = estado.copy(notificaciones = estado.notificaciones.map { it.copy(leida = true) })
        viewModelScope.launch { ids.forEach { repositorio.marcarLeida(it) } }
    }

    fun eliminar(id: Int) {
        estado = estado.copy(notificaciones = estado.notificaciones.filterNot { it.id == id })
        viewModelScope.launch { repositorio.eliminar(id) }
    }

    private suspend fun contarPendientes(): Int? = when (rol) {
        Rol.APRENDIZ -> (tareas.listar() as? Resultado.Ok)?.dato
            ?.count { it.estado in setOf("ASIGNADA", "EN_PROGRESO", "DEVUELTA") }
        Rol.INSTRUCTOR -> (revision.porRevisar() as? Resultado.Ok)?.dato?.size
        Rol.ADMINISTRADOR -> 0
    }
}
