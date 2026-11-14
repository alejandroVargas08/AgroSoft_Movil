package co.edu.sena.agrosoft.features.terreno.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import co.edu.sena.agrosoft.features.terreno.data.Analisis
import co.edu.sena.agrosoft.features.terreno.data.Captura
import co.edu.sena.agrosoft.features.terreno.data.Dron
import co.edu.sena.agrosoft.features.terreno.data.Lote
import co.edu.sena.agrosoft.features.terreno.data.SubLote
import co.edu.sena.agrosoft.features.terreno.data.TerrenoRepositorio
import co.edu.sena.agrosoft.features.terreno.data.Vuelo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class PestanaTerreno(val etiqueta: String) { VUELOS("Vuelos de dron"), LOTES("Lotes") }

enum class FiltroVuelos(val etiqueta: String) {
    TODOS("Todos"),
    ANALIZADOS("Analizados"),
    EN_PROCESO("En proceso"),
    SIN_ANALISIS("Sin análisis"),
}

enum class Capa(val etiqueta: String) {
    RGB("RGB"),
    NDVI("NDVI"),
    NDRE("NDRE"),
    GNDVI("GNDVI"),
    CLASIFICACION("Clasificación"),
}

data class TerrenoEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val vuelos: List<Vuelo> = emptyList(),
    val lotes: List<Lote> = emptyList(),
    val drones: List<Dron> = emptyList(),
    val pestana: PestanaTerreno = PestanaTerreno.VUELOS,
    val filtro: FiltroVuelos = FiltroVuelos.TODOS,
    val loteFiltro: Int? = null,
    val loteMapa: Int? = null,
    val subloteMapa: Int? = null,
    val sublotes: Map<Int, List<SubLote>> = emptyMap(),
    val cargandoSublotes: Boolean = false,
    val errorSublotes: String? = null,
    val portada: Pair<Int, Int>? = null,
    val seleccionadoId: Int? = null,
    val cargandoDetalle: Boolean = false,
    val errorDetalle: String? = null,
    val capturas: List<Captura> = emptyList(),
    val analisis: Analisis? = null,
    val capturaActual: Int = 0,
    val capa: Capa = Capa.NDVI,
    val solicitando: Boolean = false,
    val aviso: String? = null,
) {
    val visibles: List<Vuelo>
        get() = vuelos
            .filter { loteFiltro == null || it.loteId == loteFiltro }
            .filter {
                when (filtro) {
                    FiltroVuelos.TODOS -> true
                    FiltroVuelos.ANALIZADOS -> it.analizado
                    FiltroVuelos.EN_PROCESO -> it.enProceso
                    FiltroVuelos.SIN_ANALISIS -> it.ultimoAnalisis == null
                }
            }
    val loteElegido: Lote? get() = lotes.firstOrNull { it.id == loteMapa }
    val sublotesElegidos: List<SubLote> get() = loteMapa?.let { sublotes[it] }.orEmpty()
    val destacado: Vuelo? get() = vuelos.firstOrNull { it.analizado }
    val seleccionado: Vuelo? get() = vuelos.firstOrNull { it.id == seleccionadoId }
    val captura: Captura? get() = capturas.getOrNull(capturaActual.coerceIn(0, (capturas.size - 1).coerceAtLeast(0)))

    fun nombreLote(id: Int?): String = lotes.firstOrNull { it.id == id }?.nombre ?: id?.let { "Lote $it" } ?: "Sin lote"
    fun nombreDron(id: Int?): String? = drones.firstOrNull { it.id == id }?.let { it.modelo ?: it.nombre }
}

class TerrenoViewModel(
    private val repositorio: TerrenoRepositorio = TerrenoRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(TerrenoEstado())
        private set

    fun cargar() {
        viewModelScope.launch { cargarLista() }
    }

    private suspend fun cargarLista() {
        estado = estado.copy(cargando = estado.vuelos.isEmpty(), error = null)
        coroutineScope {
            val v = async { repositorio.vuelos() }
            val l = async { repositorio.lotes() }
            val d = async { repositorio.drones() }
            estado = when (val r = v.await()) {
                is Resultado.Ok -> estado.copy(
                    cargando = false,
                    vuelos = r.dato.sortedByDescending { it.fechaVuelo.orEmpty() },
                    lotes = l.await() ?: estado.lotes,
                    drones = d.await() ?: estado.drones,
                )
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
        if (estado.loteMapa == null || estado.lotes.none { it.id == estado.loteMapa }) {
            estado.lotes.firstOrNull { it.vertices.size >= 3 }?.let { seleccionarLoteMapa(it.id) }
        } else {
            estado.loteMapa?.let { cargarSublotes(it) }
        }
        cargarPortada()
    }

    private suspend fun cargarPortada() {
        val v = estado.destacado ?: return
        if (estado.portada?.first == v.id) return
        val r = repositorio.capturas(v.id)
        val primera = (r as? Resultado.Ok)?.dato?.firstOrNull() ?: return
        estado = estado.copy(portada = v.id to primera.id)
    }

    fun cambiarPestana(p: PestanaTerreno) { estado = estado.copy(pestana = p) }

    fun seleccionarLoteMapa(id: Int) {
        if (id != estado.loteMapa) estado = estado.copy(loteMapa = id, subloteMapa = null, errorSublotes = null)
        cargarSublotes(id)
    }

    fun seleccionarSublote(id: Int?) {
        estado = estado.copy(subloteMapa = if (id == estado.subloteMapa) null else id)
    }

    private fun cargarSublotes(loteId: Int) {
        if (estado.sublotes.containsKey(loteId)) return
        estado = estado.copy(cargandoSublotes = true, errorSublotes = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.sublotes(loteId)) {
                is Resultado.Ok -> estado.copy(cargandoSublotes = false, sublotes = estado.sublotes + (loteId to r.dato))
                is Resultado.Falla -> estado.copy(cargandoSublotes = false, errorSublotes = r.mensaje)
            }
        }
    }

    fun cambiarFiltro(f: FiltroVuelos) { estado = estado.copy(filtro = f) }

    fun verLote(id: Int?) {
        estado = estado.copy(loteFiltro = id, pestana = PestanaTerreno.VUELOS, filtro = FiltroVuelos.TODOS)
    }

    fun limpiarSeleccion() {
        estado = estado.copy(seleccionadoId = null)
    }

    fun abrir(vueloId: Int) {
        estado = estado.copy(
            seleccionadoId = vueloId,
            capturas = emptyList(),
            analisis = null,
            capturaActual = 0,
            capa = Capa.NDVI,
            aviso = null,
            errorDetalle = null,
        )
        cargarDetalle()
    }

    fun abrirUltimo() {
        viewModelScope.launch {
            if (estado.vuelos.isEmpty()) cargarLista()
            val v = estado.destacado ?: estado.vuelos.firstOrNull()
            if (v == null) {
                estado = estado.copy(errorDetalle = "Todavía no hay vuelos de dron registrados.")
            } else {
                abrir(v.id)
            }
        }
    }

    fun cargarDetalle(silencioso: Boolean = false) {
        val id = estado.seleccionadoId ?: return
        if (!silencioso) estado = estado.copy(cargandoDetalle = true, errorDetalle = null)
        viewModelScope.launch {
            val (capturas, analisis) = coroutineScope {
                val c = async { repositorio.capturas(id) }
                val a = async { repositorio.analisis(id) }
                c.await() to a.await()
            }
            val ultimo = (analisis as? Resultado.Ok)?.dato?.firstOrNull()
            estado = when (capturas) {
                is Resultado.Ok -> estado.copy(
                    cargandoDetalle = false,
                    capturas = capturas.dato,
                    analisis = ultimo,
                    capa = if (!silencioso && ultimo?.estado != "COMPLETADO") Capa.RGB else estado.capa,
                )
                is Resultado.Falla -> estado.copy(cargandoDetalle = false, errorDetalle = capturas.mensaje)
            }
            if (silencioso && ultimo?.estado == "COMPLETADO") cargarLista()
        }
    }

    fun cambiarCapa(c: Capa) { estado = estado.copy(capa = c) }
    fun cambiarCaptura(i: Int) { estado = estado.copy(capturaActual = i) }

    fun solicitarAnalisis() {
        val id = estado.seleccionadoId ?: return
        if (estado.solicitando) return
        estado = estado.copy(solicitando = true, aviso = null, errorDetalle = null)
        viewModelScope.launch {
            when (val r = repositorio.solicitarAnalisis(id)) {
                is Resultado.Ok -> {
                    estado = estado.copy(solicitando = false, aviso = "Análisis solicitado. Te avisaremos cuando termine.")
                    cargarLista()
                    cargarDetalle(silencioso = true)
                }
                is Resultado.Falla -> estado = estado.copy(solicitando = false, errorDetalle = r.mensaje)
            }
        }
    }
}
