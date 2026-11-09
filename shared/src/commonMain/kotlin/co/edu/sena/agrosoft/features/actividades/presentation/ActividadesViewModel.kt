@file:OptIn(ExperimentalTime::class)

package co.edu.sena.agrosoft.features.actividades.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.actividades.data.ActividadesRepositorio
import co.edu.sena.agrosoft.features.actividades.data.Actividad
import co.edu.sena.agrosoft.features.actividades.data.AsignacionActividad
import co.edu.sena.agrosoft.features.actividades.data.Cultivo
import co.edu.sena.agrosoft.features.inicio.data.AprendizResumen
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import co.edu.sena.agrosoft.features.terreno.data.Lote
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class Situacion { POR_REVISAR, VENCIDA, EN_CURSO, COMPLETADA, SIN_ASIGNAR, CANCELADA }

enum class FiltroActividades(val etiqueta: String) {
    TODAS("Todas"),
    POR_REVISAR("Por revisar"),
    EN_CURSO("En curso"),
    VENCIDAS("Vencidas"),
    SIN_ASIGNAR("Sin asignar"),
}

enum class Plazo(val etiqueta: String, val dias: Int?) {
    HOY("Hoy", 0),
    MANANA("Mañana", 1),
    TRES("3 días", 3),
    SEMANA("1 semana", 7),
    SIN("Sin límite", null),
}

val TIPOS_ACTIVIDAD = listOf(
    "Riego",
    "Fertilización",
    "Control de arvenses",
    "Monitoreo fitosanitario",
    "Siembra",
    "Cosecha",
    "Mantenimiento",
    "Otro",
)

data class ResumenActividad(val actividad: Actividad, val asignaciones: List<AsignacionActividad>) {
    val total: Int get() = asignaciones.size
    val porRevisar: Int get() = asignaciones.count { it.estado == "ENTREGADA" }
    val aprobadas: Int get() = asignaciones.count { it.estado == "APROBADA" }
    val entregadas: Int get() = asignaciones.count { it.estado == "ENTREGADA" || it.estado == "APROBADA" }
    val vencidas: Int get() = asignaciones.count { it.atrasada }
    val fechaLimite: String? get() = asignaciones.mapNotNull { it.fechaLimite }.maxOrNull()
    val situacion: Situacion
        get() = when {
            actividad.estado == "Cancelada" -> Situacion.CANCELADA
            total == 0 -> Situacion.SIN_ASIGNAR
            porRevisar > 0 -> Situacion.POR_REVISAR
            vencidas > 0 -> Situacion.VENCIDA
            aprobadas == total -> Situacion.COMPLETADA
            else -> Situacion.EN_CURSO
        }
}

data class Formulario(
    val nombre: String = "",
    val tipo: String? = null,
    val loteId: Int? = null,
    val cultivoId: Int? = null,
    val horas: String = "1",
    val valorHora: String = "",
    val descripcion: String = "",
    val aprendices: Set<Int> = emptySet(),
    val plazo: Plazo = Plazo.TRES,
)

data class ActividadesEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val items: List<ResumenActividad> = emptyList(),
    val filtro: FiltroActividades = FiltroActividades.TODAS,
    val aviso: String? = null,
    val lotes: List<Lote> = emptyList(),
    val aprendices: List<AprendizResumen> = emptyList(),
    val seleccionadaId: Int? = null,
    val cargandoDetalle: Boolean = false,
    val errorDetalle: String? = null,
    val avisoDetalle: String? = null,
    val asignaciones: List<AsignacionActividad> = emptyList(),
    val panelAsignar: Boolean = false,
    val seleccionAsignar: Set<Int> = emptySet(),
    val plazoAsignar: Plazo = Plazo.TRES,
    val procesando: Boolean = false,
    val confirmarCancelar: Boolean = false,
    val creando: Boolean = false,
    val form: Formulario = Formulario(),
    val cultivos: List<Cultivo> = emptyList(),
    val cargandoCultivos: Boolean = false,
    val guardando: Boolean = false,
    val errorForm: String? = null,
) {
    val visibles: List<ResumenActividad>
        get() = items.filter {
            when (filtro) {
                FiltroActividades.TODAS -> true
                FiltroActividades.POR_REVISAR -> it.situacion == Situacion.POR_REVISAR
                FiltroActividades.EN_CURSO -> it.situacion == Situacion.EN_CURSO
                FiltroActividades.VENCIDAS -> it.vencidas > 0
                FiltroActividades.SIN_ASIGNAR -> it.total == 0 && it.actividad.activa
            }
        }
    val seleccionada: ResumenActividad?
        get() = items.firstOrNull { it.actividad.id == seleccionadaId }?.copy(asignaciones = asignaciones)
    val disponiblesParaAsignar: List<AprendizResumen>
        get() = aprendices.filter { a -> asignaciones.none { it.aprendiz?.id == a.id } }

    fun nombreLote(id: Int?): String = lotes.firstOrNull { it.id == id }?.nombre ?: id?.let { "Lote $it" } ?: "Sin lote"
}

class ActividadesViewModel(
    private val repositorio: ActividadesRepositorio = ActividadesRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(ActividadesEstado())
        private set

    fun cargar() {
        estado = estado.copy(cargando = estado.items.isEmpty(), error = null)
        viewModelScope.launch {
            coroutineScope {
                val a = async { repositorio.listar() }
                val l = async { repositorio.lotes() }
                when (val r = a.await()) {
                    is Resultado.Ok -> {
                        val recientes = r.dato.sortedByDescending { it.fecha.orEmpty() }.take(40)
                        val items = recientes.map { act ->
                            async {
                                val asg = (repositorio.asignaciones(act.id) as? Resultado.Ok)?.dato.orEmpty()
                                ResumenActividad(act, asg)
                            }
                        }.awaitAll()
                        estado = estado.copy(cargando = false, items = items, lotes = l.await() ?: estado.lotes)
                    }
                    is Resultado.Falla -> estado = estado.copy(cargando = false, error = r.mensaje)
                }
            }
        }
    }

    fun cambiarFiltro(f: FiltroActividades) { estado = estado.copy(filtro = f) }
    fun cerrarAviso() { estado = estado.copy(aviso = null) }

    fun abrir(actividadId: Int) {
        estado = estado.copy(
            seleccionadaId = actividadId,
            asignaciones = estado.items.firstOrNull { it.actividad.id == actividadId }?.asignaciones.orEmpty(),
            errorDetalle = null,
            avisoDetalle = null,
            panelAsignar = false,
            seleccionAsignar = emptySet(),
            confirmarCancelar = false,
        )
        cargarDetalle()
    }

    fun cargarDetalle() {
        val id = estado.seleccionadaId ?: return
        estado = estado.copy(cargandoDetalle = true)
        viewModelScope.launch {
            coroutineScope {
                val a = async { repositorio.asignaciones(id) }
                val ap = async { if (estado.aprendices.isEmpty()) repositorio.aprendices() else null }
                estado = when (val r = a.await()) {
                    is Resultado.Ok -> estado.copy(cargandoDetalle = false, asignaciones = r.dato)
                    is Resultado.Falla -> estado.copy(cargandoDetalle = false, errorDetalle = r.mensaje)
                }
                ap.await()?.let { estado = estado.copy(aprendices = it) }
            }
            sincronizarItem()
        }
    }

    private fun sincronizarItem() {
        val id = estado.seleccionadaId ?: return
        estado = estado.copy(items = estado.items.map { if (it.actividad.id == id) it.copy(asignaciones = estado.asignaciones) else it })
    }

    fun quitar(asignacionId: Int) {
        val actividadId = estado.seleccionadaId ?: return
        estado = estado.copy(procesando = true, errorDetalle = null, avisoDetalle = null)
        viewModelScope.launch {
            when (val r = repositorio.quitar(actividadId, asignacionId)) {
                is Resultado.Ok -> {
                    estado = estado.copy(
                        procesando = false,
                        asignaciones = estado.asignaciones.filterNot { it.id == asignacionId },
                        avisoDetalle = "Se quitó la tarea al aprendiz.",
                    )
                    sincronizarItem()
                }
                is Resultado.Falla -> estado = estado.copy(procesando = false, errorDetalle = r.mensaje)
            }
        }
    }

    fun abrirPanelAsignar() { estado = estado.copy(panelAsignar = true, seleccionAsignar = emptySet(), avisoDetalle = null) }
    fun cerrarPanelAsignar() { estado = estado.copy(panelAsignar = false) }
    fun cambiarPlazoAsignar(p: Plazo) { estado = estado.copy(plazoAsignar = p) }

    fun alternarAsignar(id: Int) {
        val s = estado.seleccionAsignar
        estado = estado.copy(seleccionAsignar = if (id in s) s - id else s + id)
    }

    fun confirmarAsignar() {
        val actividadId = estado.seleccionadaId ?: return
        val ids = estado.seleccionAsignar.toList()
        if (ids.isEmpty()) return
        estado = estado.copy(procesando = true, errorDetalle = null)
        viewModelScope.launch {
            when (val r = repositorio.asignar(actividadId, ids, fechaLimite(estado.plazoAsignar))) {
                is Resultado.Ok -> {
                    estado = estado.copy(
                        procesando = false,
                        panelAsignar = false,
                        avisoDetalle = if (r.dato == 1) "Se asignó a 1 aprendiz." else "Se asignó a ${r.dato} aprendices.",
                    )
                    cargarDetalle()
                }
                is Resultado.Falla -> estado = estado.copy(procesando = false, errorDetalle = r.mensaje)
            }
        }
    }

    fun pedirCancelar() { estado = estado.copy(confirmarCancelar = true) }
    fun descartarCancelar() { estado = estado.copy(confirmarCancelar = false) }

    fun cancelarActividad() {
        val id = estado.seleccionadaId ?: return
        estado = estado.copy(confirmarCancelar = false, procesando = true, errorDetalle = null)
        viewModelScope.launch {
            when (val r = repositorio.cancelar(id)) {
                is Resultado.Ok -> {
                    estado = estado.copy(
                        procesando = false,
                        avisoDetalle = "La actividad quedó cancelada.",
                        items = estado.items.map {
                            if (it.actividad.id == id) it.copy(actividad = it.actividad.copy(estado = "Cancelada")) else it
                        },
                    )
                }
                is Resultado.Falla -> estado = estado.copy(procesando = false, errorDetalle = r.mensaje)
            }
        }
    }

    fun nuevaTarea() {
        estado = estado.copy(creando = true, form = Formulario(), cultivos = emptyList(), errorForm = null)
        viewModelScope.launch {
            coroutineScope {
                val l = async { if (estado.lotes.isEmpty()) repositorio.lotes() else null }
                val a = async { if (estado.aprendices.isEmpty()) repositorio.aprendices() else null }
                l.await()?.let { estado = estado.copy(lotes = it) }
                a.await()?.let { estado = estado.copy(aprendices = it) }
            }
        }
    }

    fun cancelarCreacion() { estado = estado.copy(creando = false, errorForm = null) }

    private fun editar(cambio: (Formulario) -> Formulario) {
        estado = estado.copy(form = cambio(estado.form), errorForm = null)
    }

    fun cambiarNombre(v: String) = editar { it.copy(nombre = v) }
    fun cambiarHoras(v: String) = editar { it.copy(horas = v.filter { c -> c.isDigit() || c == ',' || c == '.' }) }
    fun cambiarValorHora(v: String) = editar { it.copy(valorHora = v.filter { c -> c.isDigit() }) }
    fun cambiarDescripcion(v: String) = editar { it.copy(descripcion = v) }
    fun cambiarPlazo(p: Plazo) = editar { it.copy(plazo = p) }

    fun cambiarTipo(t: String) = editar {
        val nombreAuto = it.nombre.isBlank() || it.nombre == it.tipo
        it.copy(tipo = t, nombre = if (nombreAuto && t != "Otro") t else it.nombre)
    }

    fun alternarAprendiz(id: Int) = editar {
        it.copy(aprendices = if (id in it.aprendices) it.aprendices - id else it.aprendices + id)
    }

    fun seleccionarTodos() = editar {
        val todos = estado.aprendices.map { a -> a.id }.toSet()
        it.copy(aprendices = if (it.aprendices.size == todos.size) emptySet() else todos)
    }

    fun elegirLote(id: Int) {
        editar { it.copy(loteId = id, cultivoId = null) }
        estado = estado.copy(cargandoCultivos = true, cultivos = emptyList())
        viewModelScope.launch {
            estado = when (val r = repositorio.cultivos(id)) {
                is Resultado.Ok -> {
                    val activos = r.dato.filter { it.estado?.lowercase() != "finalizado" }
                    estado.copy(
                        cargandoCultivos = false,
                        cultivos = activos,
                        form = if (activos.size == 1) estado.form.copy(cultivoId = activos.first().id) else estado.form,
                    )
                }
                is Resultado.Falla -> estado.copy(cargandoCultivos = false, errorForm = r.mensaje)
            }
        }
    }

    fun elegirCultivo(id: Int) = editar { it.copy(cultivoId = id) }

    fun guardar() {
        val f = estado.form
        val horas = f.horas.replace(",", ".").toDoubleOrNull()
        val error = when {
            f.tipo == null -> "Elige el tipo de actividad."
            f.nombre.trim().length < 3 -> "Escribe un nombre para la actividad."
            f.loteId == null -> "Elige el lote."
            f.cultivoId == null -> if (estado.cultivos.isEmpty()) "Ese lote no tiene cultivos activos. Créalo primero en la web." else "Elige el cultivo."
            horas == null || horas <= 0 -> "Escribe las horas estimadas."
            else -> null
        }
        if (error != null) {
            estado = estado.copy(errorForm = error)
            return
        }
        estado = estado.copy(guardando = true, errorForm = null)
        viewModelScope.launch {
            val creada = repositorio.crear(
                nombre = f.nombre.trim(),
                tipo = f.tipo!!,
                loteId = f.loteId!!,
                cultivoId = f.cultivoId!!,
                fecha = hoy(),
                horas = horas!!,
                valorHora = f.valorHora.toDoubleOrNull() ?: 0.0,
                descripcion = f.descripcion.trim().ifBlank { null },
            )
            when (creada) {
                is Resultado.Falla -> estado = estado.copy(guardando = false, errorForm = creada.mensaje)
                is Resultado.Ok -> {
                    var mensaje = "Actividad \"${creada.dato.titulo}\" creada."
                    if (f.aprendices.isNotEmpty()) {
                        mensaje = when (val a = repositorio.asignar(creada.dato.id, f.aprendices.toList(), fechaLimite(f.plazo))) {
                            is Resultado.Ok -> "Actividad creada y asignada a ${a.dato} ${if (a.dato == 1) "aprendiz" else "aprendices"}."
                            is Resultado.Falla -> "Actividad creada, pero no se pudo asignar: ${a.mensaje}"
                        }
                    }
                    estado = estado.copy(guardando = false, creando = false, aviso = mensaje, filtro = FiltroActividades.TODAS)
                    cargar()
                }
            }
        }
    }

    private fun hoy(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

    private fun fechaLimite(p: Plazo): String? = p.dias?.let {
        Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(it, DateTimeUnit.DAY).toString()
    }
}
