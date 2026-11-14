package co.edu.sena.agrosoft.features.terreno.data

import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

internal fun JsonElement?.numero(): Double? =
    (this as? JsonPrimitive)?.contentOrNull?.trim()?.replace(",", ".")?.toDoubleOrNull()

@Serializable
data class ResumenAnalisisVuelo(
    val id: Int,
    val estado: String,
    val progreso: Int = 0,
    val saludablePct: JsonElement? = null,
    val capturasCriticas: Int? = null,
    val finalizadoEn: String? = null,
)

@Serializable
data class Vuelo(
    val id: Int,
    val nombre: String,
    val dronId: Int? = null,
    val fechaVuelo: String? = null,
    val alturaVueloM: JsonElement? = null,
    val loteId: Int? = null,
    val observaciones: String? = null,
    val estado: String = "REGISTRADO",
    val totalCapturas: Int = 0,
    val ultimoAnalisis: ResumenAnalisisVuelo? = null,
) {
    val analizado: Boolean get() = ultimoAnalisis?.estado == "COMPLETADO"
    val enProceso: Boolean get() = estado == "ANALIZANDO" || ultimoAnalisis?.estado in setOf("PENDIENTE", "PROCESANDO")
    val saludable: Double? get() = if (analizado) ultimoAnalisis?.saludablePct.numero() else null
    val puedeAnalizarse: Boolean get() = estado in setOf("CON_IMAGENES", "ANALIZADO", "ERROR") && totalCapturas > 0 && !enProceso
}

@Serializable
data class Punto(val lat: Double, val lng: Double)

@Serializable
data class Lote(
    val id: Int,
    val nombre: String,
    val areaHa: JsonElement? = null,
    val areaM2: JsonElement? = null,
    val descripcion: String? = null,
    val estado: String? = null,
    val centroide: Punto? = null,
    val vertices: List<Punto> = emptyList(),
)

@Serializable
data class SubLote(
    val id: Int,
    val loteId: Int,
    val nombre: String,
    val areaHa: JsonElement? = null,
    val areaM2: JsonElement? = null,
    val descripcion: String? = null,
    val estado: String? = null,
    val centroide: Punto? = null,
    val vertices: List<Punto> = emptyList(),
)

@Serializable
data class ActualizarTerreno(
    val nombre: String? = null,
    val descripcion: String? = null,
    val vertices: List<Punto>? = null,
    val centroide: Punto? = null,
    val areaM2: Double? = null,
)

@Serializable
private data class CrearSubLote(
    val loteId: Int,
    val nombre: String,
    val vertices: List<Punto>,
    val centroide: Punto,
    val areaM2: Double,
    val descripcion: String? = null,
)

@Serializable
private data class MiAcceso(val permisos: List<String> = emptyList())

@Serializable
data class Dron(
    val id: Int,
    val nombre: String,
    val modelo: String? = null,
)

@Serializable
data class Captura(
    val id: Int,
    val numeroCaptura: Int = 0,
    val completa: Boolean = false,
    val tomadaEn: String? = null,
)

@Serializable
data class EstadisticaIndice(
    val promedio: Double = 0.0,
    val minimo: Double = 0.0,
    val maximo: Double = 0.0,
)

@Serializable
data class SintesisVuelo(
    val resumen: String? = null,
    val hallazgosPrincipales: List<String> = emptyList(),
    val zonasCandidatasSiembra: String? = null,
    val recomendaciones: List<String> = emptyList(),
    val advertencias: List<String> = emptyList(),
)

@Serializable
data class ResumenIa(
    val estado: String? = null,
    val sintesis: SintesisVuelo? = null,
    val mensaje: String? = null,
)

@Serializable
data class ResumenCompleto(
    val ndvi: EstadisticaIndice? = null,
    val ndre: EstadisticaIndice? = null,
    val gndvi: EstadisticaIndice? = null,
    val ndwi: EstadisticaIndice? = null,
    val distribucionNdvi: Map<String, Double> = emptyMap(),
    val capturasAnalizadas: Int = 0,
    val capturasCriticas: Int = 0,
    val ia: ResumenIa? = null,
)

@Serializable
data class Analisis(
    val id: Int,
    val estado: String,
    val etapa: String? = null,
    val progreso: Int = 0,
    val resumen: ResumenCompleto? = null,
    val mensajeError: String? = null,
    val finalizadoEn: String? = null,
)

@Serializable
private data class SolicitudAnalisis(val usarIa: Boolean)

class TerrenoRepositorio(private val http: HttpClient = ApiCliente.http) {

    suspend fun vuelos(): Resultado<List<Vuelo>> = llamar { http.get("vuelos") }

    suspend fun lotes(): List<Lote>? = opcional { http.get("territorio/lotes") }

    suspend fun drones(): List<Dron>? = opcional { http.get("drones") }

    suspend fun capturas(vueloId: Int): Resultado<List<Captura>> = llamar { http.get("vuelos/$vueloId/capturas") }

    suspend fun analisis(vueloId: Int): Resultado<List<Analisis>> = llamar { http.get("vuelos/$vueloId/analisis") }

    suspend fun permisos(): List<String> = opcional<MiAcceso> { http.get("mi-acceso") }?.permisos ?: emptyList()

    suspend fun sublotes(loteId: Int): Resultado<List<SubLote>> =
        llamar(PROHIBIDO_LOTES) { http.get("territorio/sublotes/por-lote/$loteId") }

    suspend fun actualizarLote(id: Int, datos: ActualizarTerreno): Resultado<Lote> =
        llamar(PROHIBIDO_EDITAR) { http.patch("territorio/lotes/$id") { setBody(datos) } }

    suspend fun actualizarSublote(id: Int, datos: ActualizarTerreno): Resultado<SubLote> =
        llamar(PROHIBIDO_EDITAR) { http.patch("territorio/sublotes/$id") { setBody(datos) } }

    suspend fun crearSublote(loteId: Int, nombre: String, vertices: List<Punto>, descripcion: String? = null): Resultado<SubLote> =
        llamar(PROHIBIDO_EDITAR) {
            http.post("territorio/sublotes") {
                setBody(CrearSubLote(loteId, nombre, vertices, centroide(vertices), areaPoligonoM2(vertices), descripcion))
            }
        }

    suspend fun solicitarAnalisis(vueloId: Int): Resultado<Unit> = try {
        val r = http.post("vuelos/$vueloId/analisis") { setBody(SolicitudAnalisis(usarIa = true)) }
        if (r.status.isSuccess()) Resultado.Ok(Unit) else Resultado.Falla(mensajeDe(r))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor.")
    }

    private suspend inline fun <reified T> llamar(
        prohibido: String = PROHIBIDO_VUELOS,
        bloque: suspend () -> HttpResponse,
    ): Resultado<T> = try {
        val r = bloque()
        when {
            r.status.isSuccess() -> Resultado.Ok(r.body<T>())
            r.status == HttpStatusCode.Forbidden -> Resultado.Falla(prohibido)
            else -> Resultado.Falla(mensajeDe(r))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }

    private suspend inline fun <reified T> opcional(bloque: suspend () -> HttpResponse): T? = try {
        val r = bloque()
        if (r.status.isSuccess()) r.body<T>() else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun mensajeDe(respuesta: HttpResponse): String {
        val cuerpo = runCatching { respuesta.body<JsonObject>() }.getOrNull()
        return when (val m = cuerpo?.get("message")) {
            is JsonArray -> m.firstOrNull()?.jsonPrimitive?.contentOrNull
            null -> null
            else -> m.jsonPrimitive.contentOrNull
        } ?: "No se pudo completar la acción."
    }

    private companion object {
        const val PROHIBIDO_VUELOS = "No tienes permiso para ver los vuelos de dron."
        const val PROHIBIDO_LOTES = "No tienes permiso para ver los lotes."
        const val PROHIBIDO_EDITAR = "No tienes permiso para editar lotes."
    }
}
