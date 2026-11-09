package co.edu.sena.agrosoft.features.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.admin.data.InsumoBajo
import co.edu.sena.agrosoft.features.sensores.data.EstadoSensor
import co.edu.sena.agrosoft.features.sensores.data.Sensor
import co.edu.sena.agrosoft.features.sensores.presentation.conUnidad
import co.edu.sena.agrosoft.features.sensores.presentation.iconoDe
import com.composables.icons.lucide.*
import kotlin.math.roundToInt

@Composable
fun AlertasScreen(vm: AdminViewModel, onAbrirSensor: (Int) -> Unit, onAbrirInsumo: () -> Unit) {
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        when {
            e.cargando -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
            e.totalAlertas == 0 -> MensajeAdmin(Lucide.CircleCheck, "Todo en orden. No hay alertas activas.")
            else -> {
                Text(
                    if (e.totalAlertas == 1) "1 alerta activa" else "${e.totalAlertas} alertas activas",
                    fontSize = 12.sp,
                    color = AgroColors.TextSecondary,
                )
                if (e.sensoresEnAlerta.isNotEmpty()) {
                    TituloSeccion("Sensores")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        e.sensoresEnAlerta
                            .sortedBy { if (it.situacion == EstadoSensor.DESCONECTADO) 1 else 0 }
                            .forEach { s -> AlertaSensor(s) { onAbrirSensor(s.id) } }
                    }
                }
                if (e.insumos.isNotEmpty()) {
                    TituloSeccion("Inventario")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        e.insumos.forEach { i -> AlertaInsumo(i, onAbrirInsumo) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertaSensor(s: Sensor, onClick: () -> Unit) {
    val critica = s.situacion == EstadoSensor.ALTO || s.situacion == EstadoSensor.BAJO
    val (titulo, icono) = when (s.situacion) {
        EstadoSensor.ALTO -> "Por encima del máximo" to iconoDe(s).first
        EstadoSensor.BAJO -> "Por debajo del mínimo" to iconoDe(s).first
        else -> "Estación desconectada" to Lucide.WifiOff
    }
    val detalle = when (s.situacion) {
        EstadoSensor.ALTO -> "${s.nombreSensor} · ${conUnidad(s.valorActual, s)} (máx. ${conUnidad(s.maximo, s)})"
        EstadoSensor.BAJO -> "${s.nombreSensor} · ${conUnidad(s.valorActual, s)} (mín. ${conUnidad(s.minimo, s)})"
        else -> "${s.nombreSensor} · ${Fechas.haceCuanto(s.ultimaVistaEn ?: s.ultimaMedicion)?.let { "sin datos desde ${it.lowercase()}" } ?: "sin datos"}"
    }
    Fila(
        icono = icono,
        color = if (critica) AgroColors.Error else AgroColors.Accent,
        fondo = if (critica) AgroColors.ErrorSoft else AgroColors.AccentSoft,
        borde = if (critica) Color(0xFFFECACA) else AgroColors.BorderCard,
        titulo = titulo,
        detalle = detalle,
        accion = "Activa · Ver detalle",
        onClick = onClick,
    )
}

@Composable
private fun AlertaInsumo(i: InsumoBajo, onClick: () -> Unit) {
    val unidad = i.unidadUso.orEmpty()
    Fila(
        icono = Lucide.Package,
        color = AgroColors.Accent,
        fondo = AgroColors.AccentSoft,
        borde = AgroColors.BorderCard,
        titulo = "Insumo bajo mínimo",
        detalle = "${i.nombre} · quedan ${i.disponible.roundToInt()} $unidad (mínimo ${i.minimo.roundToInt()})",
        accion = "Gestionar en la web",
        onClick = onClick,
    )
}

@Composable
private fun Fila(
    icono: ImageVector,
    color: Color,
    fondo: Color,
    borde: Color,
    titulo: String,
    detalle: String,
    accion: String,
    onClick: () -> Unit,
) {
    Tarjeta(modifier = Modifier.fillMaxWidth(), borde = borde, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(fondo),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title, modifier = Modifier.weight(1f))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AgroColors.Error))
                }
                Spacer(Modifier.height(3.dp))
                Text(detalle, fontSize = 11.sp, color = AgroColors.TextSecondary, lineHeight = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(accion, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Primary)
            }
        }
    }
}
