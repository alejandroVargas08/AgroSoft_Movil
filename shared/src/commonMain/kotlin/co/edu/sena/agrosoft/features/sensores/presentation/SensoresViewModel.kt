@file:OptIn(ExperimentalTime::class)

package co.edu.sena.agrosoft.features.sensores.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.sensores.data.AlertaSensor
import co.edu.sena.agrosoft.features.sensores.data.EstadoSensor
import co.edu.sena.agrosoft.features.sensores.data.Lectura
import co.edu.sena.agrosoft.features.sensores.data.Sensor
import co.edu.sena.agrosoft.features.sensores.data.SensoresRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class FiltroSensores(val etiqueta: String) {
    TODOS("Todos"),
    ALERTA("Con alerta"),
    DESCONECTADOS("Desconectados"),
}

enum class Periodo(val etiqueta: String, val horas: Long?) {
    DIA("24 h", 24),
    SEMANA("7 días", 24 * 7),
    MES("30 días", 24 * 30),
    TODO("Todo", null),
}

data class SensoresEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val sensores: List<Sensor> = emptyList(),
    val filtro: FiltroSensores = FiltroSensores.TODOS,
    val seleccionadoId: Int? = null,
    val cargandoDetalle: Boolean = false,
    val errorDetalle: String? = null,
    val lecturas: List<Lectura> = emptyList(),
    val alertas: List<AlertaSensor> = emptyList(),
    val periodo: Periodo = Periodo.DIA,
) {
    val visibles: List<Sensor>
        get() = when (filtro) {
            FiltroSensores.TODOS -> sensores
            FiltroSensores.ALERTA -> sensores.filter { it.situacion == EstadoSensor.ALTO || it.situacion == EstadoSensor.BAJO }
            FiltroSensores.DESCONECTADOS -> sensores.filter { it.situacion == EstadoSensor.DESCONECTADO || it.situacion == EstadoSensor.INACTIVO }
        }
    val conectados: Int get() = sensores.count { it.activo && it.conectado }
    val desconectados: Int get() = sensores.count { !it.activo || !it.conectado }
    val conAlerta: Int get() = sensores.count { it.situacion == EstadoSensor.ALTO || it.situacion == EstadoSensor.BAJO }
    val seleccionado: Sensor? get() = sensores.firstOrNull { it.id == seleccionadoId }
    val puntos: List<Pair<Long, Double>> get() = puntosDe(lecturas, periodo)
    val alertasDelPeriodo: List<AlertaSensor> get() = alertasDe(alertas, periodo)
}

class SensoresViewModel(
    private val repositorio: SensoresRepositorio = SensoresRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(SensoresEstado())
        private set

    fun cargar() {
        estado = estado.copy(cargando = estado.sensores.isEmpty(), error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.listar()) {
                is Resultado.Ok -> estado.copy(
                    cargando = false,
                    sensores = r.dato.sortedWith(
                        compareByDescending<Sensor> { it.enAlerta }.thenBy { it.nombreSensor.lowercase() }
                    ),
                )
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun cambiarFiltro(filtro: FiltroSensores) {
        estado = estado.copy(filtro = filtro)
    }

    fun abrir(sensorId: Int) {
        estado = estado.copy(
            seleccionadoId = sensorId,
            cargandoDetalle = true,
            errorDetalle = null,
            lecturas = emptyList(),
            alertas = emptyList(),
        )
        cargarDetalle()
    }

    fun cargarDetalle() {
        val id = estado.seleccionadoId ?: return
        estado = estado.copy(cargandoDetalle = estado.lecturas.isEmpty(), errorDetalle = null)
        viewModelScope.launch {
            val (lecturas, alertas) = coroutineScope {
                val l = async { repositorio.lecturas(id) }
                val a = async { repositorio.alertas(id) }
                l.await() to a.await()
            }
            estado = when (lecturas) {
                is Resultado.Ok -> {
                    val periodo = Periodo.entries.firstOrNull { puntosDe(lecturas.dato, it).size >= 2 } ?: Periodo.TODO
                    estado.copy(
                        cargandoDetalle = false,
                        lecturas = lecturas.dato,
                        alertas = (alertas as? Resultado.Ok)?.dato.orEmpty(),
                        periodo = periodo,
                    )
                }
                is Resultado.Falla -> estado.copy(cargandoDetalle = false, errorDetalle = lecturas.mensaje)
            }
        }
    }

    fun cambiarPeriodo(periodo: Periodo) {
        estado = estado.copy(periodo = periodo)
    }
}

private fun milis(texto: String?): Long? = Fechas.instante(texto)?.toEpochMilliseconds()

private fun desdeDe(periodo: Periodo): Long? =
    periodo.horas?.let { Clock.System.now().toEpochMilliseconds() - it * 3_600_000L }

internal fun puntosDe(lecturas: List<Lectura>, periodo: Periodo): List<Pair<Long, Double>> {
    val desde = desdeDe(periodo)
    return lecturas
        .mapNotNull { l ->
            val t = milis(l.fechaLectura) ?: return@mapNotNull null
            val v = l.numero ?: return@mapNotNull null
            t to v
        }
        .filter { desde == null || it.first >= desde }
        .sortedBy { it.first }
}

internal fun alertasDe(alertas: List<AlertaSensor>, periodo: Periodo): List<AlertaSensor> {
    val desde = desdeDe(periodo)
    return alertas.filter { a -> desde == null || (milis(a.fechaAlerta) ?: 0L) >= desde }
}
