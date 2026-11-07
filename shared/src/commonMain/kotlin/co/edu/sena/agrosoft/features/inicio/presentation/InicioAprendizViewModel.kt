package co.edu.sena.agrosoft.features.inicio.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.inicio.data.Asignacion
import co.edu.sena.agrosoft.features.inicio.data.DatosInicioAprendiz
import co.edu.sena.agrosoft.features.inicio.data.InicioRepositorio
import co.edu.sena.agrosoft.features.inicio.data.SensorDto
import co.edu.sena.agrosoft.features.inicio.data.comoDouble
import kotlinx.coroutines.launch

data class TareaProxima(
    val id: Int,
    val titulo: String,
    val detalle: String,
    val etiqueta: String?,
    val devuelta: Boolean,
)

data class ClimaResumen(
    val estacion: String,
    val temperatura: Double?,
    val humedad: Double?,
    val uv: Double?,
    val conectada: Boolean,
    val actualizado: String?,
    val enAlerta: Int,
)

data class VueloResumen(val id: Int, val nombre: String, val saludable: Double)

data class InicioAprendizEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val proxima: TareaProxima? = null,
    val sinTareas: Boolean = false,
    val clima: ClimaResumen? = null,
    val vuelo: VueloResumen? = null,
    val pendientes: Int = 0,
    val porVencer: Int = 0,
    val devueltas: Int = 0,
)

class InicioAprendizViewModel(
    private val repositorio: InicioRepositorio = InicioRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(InicioAprendizEstado())
        private set

    init {
        cargar()
    }

    fun cargar() {
        estado = estado.copy(cargando = true, error = null)
        viewModelScope.launch {
            val datos = repositorio.cargarAprendiz()
            estado = if (datos.tareas == null && datos.sensores == null && datos.vuelos == null) {
                estado.copy(cargando = false, error = "No se pudo cargar tu información. Revisa tu conexión.")
            } else {
                construir(datos)
            }
        }
    }

    private fun construir(d: DatosInicioAprendiz): InicioAprendizEstado {
        val tareas = d.tareas.orEmpty()
        val activas = tareas.filter { it.estado in setOf("ASIGNADA", "EN_PROGRESO", "DEVUELTA") }
        val proxima = activas.sortedWith(compareBy(nullsLast()) { Fechas.instante(it.fechaLimite) }).firstOrNull()

        return InicioAprendizEstado(
            cargando = false,
            proxima = proxima?.let(::aTareaProxima),
            sinTareas = d.tareas != null && activas.isEmpty(),
            clima = d.sensores?.let(::aClima),
            vuelo = d.vuelos
                ?.filter { it.ultimoAnalisis?.saludablePct.comoDouble() != null }
                ?.maxByOrNull { Fechas.instante(it.fechaVuelo)?.toEpochMilliseconds() ?: 0L }
                ?.let { VueloResumen(it.id, it.nombre, it.ultimoAnalisis?.saludablePct.comoDouble() ?: 0.0) },
            pendientes = tareas.count { it.estado == "ASIGNADA" || it.estado == "EN_PROGRESO" },
            porVencer = activas.count { Fechas.horasHasta(it.fechaLimite)?.let { h -> h in 0..24 } == true },
            devueltas = tareas.count { it.estado == "DEVUELTA" },
        )
    }

    private fun aTareaProxima(a: Asignacion): TareaProxima {
        val etiqueta = when {
            a.estado == "DEVUELTA" -> "DEVUELTA"
            else -> Fechas.etiquetaLimite(a.fechaLimite)?.uppercase()
        }
        return TareaProxima(
            id = a.id,
            titulo = a.actividad?.nombre ?: "Tarea asignada",
            detalle = a.actividad?.tipo?.replaceFirstChar { it.uppercase() } ?: "",
            etiqueta = etiqueta,
            devuelta = a.estado == "DEVUELTA",
        )
    }

    private fun aClima(sensores: List<SensorDto>): ClimaResumen? {
        if (sensores.isEmpty()) return null
        fun busca(vararg claves: String) = sensores.firstOrNull { s ->
            val texto = "${s.tipo.orEmpty()} ${s.nombreSensor.orEmpty()}".lowercase()
            claves.any { it in texto } && s.ultimoValor.comoDouble() != null
        }
        val temp = busca("temp")
        val hum = busca("hum")
        val uv = busca("uv")
        val ubicacion = (temp ?: hum ?: sensores.first()).nombreSensor?.substringAfter(" - ", "")?.trim().orEmpty()
        val masReciente = sensores.mapNotNull { it.ultimaMedicion }.maxByOrNull { Fechas.instante(it)?.toEpochMilliseconds() ?: 0L }
        val enAlerta = sensores.count { s ->
            val v = s.ultimoValor.comoDouble() ?: return@count false
            val min = s.valorMinimoSensor.comoDouble()
            val max = s.valorMaximoSensor.comoDouble()
            (min != null && v < min) || (max != null && v > max)
        }
        return ClimaResumen(
            estacion = if (ubicacion.isNotBlank()) "Estación $ubicacion" else "Sensores de la finca",
            temperatura = temp?.ultimoValor.comoDouble(),
            humedad = hum?.ultimoValor.comoDouble(),
            uv = uv?.ultimoValor.comoDouble(),
            conectada = sensores.any { it.estadoConexion.equals("conectado", ignoreCase = true) },
            actualizado = Fechas.haceCuanto(masReciente),
            enAlerta = enAlerta,
        )
    }
}
