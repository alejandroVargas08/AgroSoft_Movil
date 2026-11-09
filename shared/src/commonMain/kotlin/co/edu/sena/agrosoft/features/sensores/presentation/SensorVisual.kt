@file:OptIn(ExperimentalTime::class)

package co.edu.sena.agrosoft.features.sensores.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.features.sensores.data.EstadoSensor
import co.edu.sena.agrosoft.features.sensores.data.Sensor
import com.composables.icons.lucide.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class Medida(val nombre: String, val decimales: Int) {
    TEMPERATURA("Temperatura", 1),
    HUMEDAD("Humedad", 1),
    UV("Índice UV", 1),
    OTRA("Valor", 1),
}

internal fun medidaDe(sensor: Sensor): Medida {
    val unidad = sensor.unidad.orEmpty().trim().lowercase()
    when {
        unidad in setOf("°c", "ºc", "c", "°", "celsius") -> return Medida.TEMPERATURA
        unidad in setOf("%", "%rh", "rh", "% hr", "%hr") -> return Medida.HUMEDAD
        "uv" in unidad || "índice" in unidad || "indice" in unidad -> return Medida.UV
    }
    val texto = "${sensor.tipo.orEmpty()} ${sensor.nombreSensor}".lowercase()
    return when {
        "uv" in texto || "30a" in texto || "radiaci" in texto -> Medida.UV
        "hum" in texto -> Medida.HUMEDAD
        "temp" in texto -> Medida.TEMPERATURA
        "dht" in texto -> Medida.TEMPERATURA
        else -> Medida.OTRA
    }
}

internal fun modeloDe(sensor: Sensor): String? {
    val texto = "${sensor.tipo.orEmpty()} ${sensor.nombreSensor}".lowercase()
    return when {
        "dht22" in texto || "dht-22" in texto -> "DHT22"
        "uvm" in texto || "30a" in texto -> "UVM-30A"
        else -> null
    }
}

internal fun iconoDe(sensor: Sensor): Pair<ImageVector, Color> = when (medidaDe(sensor)) {
    Medida.TEMPERATURA -> Lucide.Thermometer to AgroColors.Error
    Medida.HUMEDAD -> Lucide.Droplets to AgroColors.Info
    Medida.UV -> Lucide.Sun to AgroColors.Accent
    Medida.OTRA -> Lucide.Radio to AgroColors.Primary
}

data class NivelUv(val etiqueta: String, val color: Color, val fondo: Color)

fun nivelUv(indice: Double): NivelUv = when {
    indice < 3 -> NivelUv("Bajo", Color(0xFF15803D), Color(0xFFDCFCE7))
    indice < 6 -> NivelUv("Moderado", Color(0xFFA16207), Color(0xFFFEF9C3))
    indice < 8 -> NivelUv("Alto", Color(0xFFC2410C), Color(0xFFFFEDD5))
    indice < 11 -> NivelUv("Muy alto", Color(0xFFB91C1C), Color(0xFFFEE2E2))
    else -> NivelUv("Extremo", Color(0xFF7E22CE), Color(0xFFF3E8FF))
}

internal fun formatear(valor: Double, decimales: Int?): String {
    val d = (decimales ?: 1).coerceIn(0, 3)
    if (d == 0) return valor.roundToLong().toString()
    val factor = 10.0.pow(d).toLong()
    val r = (valor * factor).roundToLong()
    val a = abs(r)
    val signo = if (r < 0) "-" else ""
    return "$signo${a / factor},${(a % factor).toString().padStart(d, '0')}"
}

internal fun conUnidad(valor: Double?, sensor: Sensor): String {
    if (valor == null) return "—"
    val medida = medidaDe(sensor)
    val numero = formatear(valor, sensor.decimales ?: medida.decimales)
    return when (medida) {
        Medida.TEMPERATURA -> "$numero °C"
        Medida.HUMEDAD -> "$numero %"
        Medida.UV -> "UV $numero"
        Medida.OTRA -> sensor.unidad?.trim()?.takeIf { it.isNotEmpty() }?.let { "$numero $it" } ?: numero
    }
}

internal fun textoSituacion(s: EstadoSensor): Pair<String, Color> = when (s) {
    EstadoSensor.NORMAL -> "Dentro del rango" to AgroColors.Primary
    EstadoSensor.ALTO -> "Por encima del máximo" to AgroColors.Error
    EstadoSensor.BAJO -> "Por debajo del mínimo" to AgroColors.Warning
    EstadoSensor.DESCONECTADO -> "Sin conexión" to AgroColors.TextSecondary
    EstadoSensor.INACTIVO -> "Sensor inactivo" to AgroColors.TextSecondary
    EstadoSensor.SIN_DATOS -> "Sin lecturas" to AgroColors.TextSecondary
}

internal fun fechaCorta(milis: Long, conDia: Boolean): String {
    val f = Instant.fromEpochMilliseconds(milis).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = if (f.hour % 12 == 0) 12 else f.hour % 12
    val hora = "$h:${f.minute.toString().padStart(2, '0')} ${if (f.hour < 12) "a. m." else "p. m."}"
    if (!conDia) return hora
    val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    return "${f.day} ${meses[f.month.ordinal]} · $hora"
}

@Composable
internal fun ChipConexion(sensor: Sensor) {
    val (texto, fondo, color, icono) = when {
        !sensor.activo -> Cuarteta("Inactivo", Color(0xFFF1F5F9), AgroColors.TextSecondary, Lucide.WifiOff)
        sensor.conectado -> Cuarteta("Conectado", AgroColors.SuccessSoft, AgroColors.Primary, Lucide.Wifi)
        else -> Cuarteta("Desconectado", Color(0xFFF1F5F9), AgroColors.TextSecondary, Lucide.WifiOff)
    }
    Row(
        Modifier.clip(CircleShape).background(fondo).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

internal data class Cuarteta(val a: String, val b: Color, val c: Color, val d: ImageVector)
