package co.edu.sena.agrosoft.features.sensores.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.sensores.data.EstadoSensor
import co.edu.sena.agrosoft.features.sensores.data.Sensor
import com.composables.icons.lucide.*

@Composable
fun SensoresScreen(vm: SensoresViewModel, onAbrir: (Int) -> Unit) {
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        when {
            e.cargando -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
            e.error != null && e.sensores.isEmpty() -> Mensaje(Lucide.WifiOff, e.error, "Reintentar", vm::cargar)
            e.sensores.isEmpty() -> Mensaje(Lucide.Radio, "Aún no hay sensores registrados.", null, null)
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Contador(e.conectados.toString(), "Conectados", AgroColors.Title, Modifier.weight(1f))
                    Contador(e.desconectados.toString(), "Desconectados", AgroColors.Title, Modifier.weight(1f))
                    Contador(e.conAlerta.toString(), "Con alerta", if (e.conAlerta > 0) AgroColors.Error else AgroColors.Title, Modifier.weight(1f))
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FiltroSensores.entries.forEach { f ->
                        val activo = f == e.filtro
                        Text(
                            f.etiqueta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activo) Color.White else AgroColors.TextMuted,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (activo) AgroColors.Primary else Color.White)
                                .border(1.dp, if (activo) AgroColors.Primary else AgroColors.Border, CircleShape)
                                .clickable { vm.cambiarFiltro(f) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                if (e.visibles.isEmpty()) {
                    Mensaje(Lucide.CircleCheck, "No hay sensores en este filtro.", null, null)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        e.visibles.forEach { s -> TarjetaSensor(s) { onAbrir(s.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Contador(valor: String, etiqueta: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, AgroColors.BorderCard, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(etiqueta, fontSize = 10.sp, color = AgroColors.TextMuted)
    }
}

@Composable
private fun TarjetaSensor(s: Sensor, onClick: () -> Unit) {
    val (icono, colorIcono) = iconoDe(s)
    val (textoEstado, colorEstado) = textoSituacion(s.situacion)
    val alerta = s.situacion == EstadoSensor.ALTO || s.situacion == EstadoSensor.BAJO

    Tarjeta(
        modifier = Modifier.fillMaxWidth(),
        borde = if (alerta) Color(0xFFFECACA) else AgroColors.BorderCard,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(colorIcono.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.nombreSensor, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (s.loteId != null) {
                            Icon(Lucide.MapPin, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            listOfNotNull(medidaDe(s).nombre, modeloDe(s), s.loteId?.let { "Lote $it" }).joinToString(" · "),
                            fontSize = 11.sp,
                            color = AgroColors.TextMuted,
                        )
                    }
                }
                ChipConexion(s)
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(conUnidad(s.valorActual, s), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                Spacer(Modifier.width(10.dp))
                Text(textoEstado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colorEstado, modifier = Modifier.padding(bottom = 5.dp))
            }

            BarraRango(s)

            HorizontalDivider(color = AgroColors.Border, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Fechas.haceCuanto(s.ultimaMedicion ?: s.ultimaVistaEn)?.let { "Última lectura: ${it.lowercase()}" } ?: "Sin lecturas todavía",
                    fontSize = 11.sp,
                    color = if (s.conectado) AgroColors.Primary else AgroColors.TextMuted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(Lucide.ChevronRight, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun BarraRango(s: Sensor) {
    val min = s.minimo
    val max = s.maximo
    val v = s.valorActual
    if (min == null || max == null || max <= min) return
    Spacer(Modifier.height(10.dp))
    val fraccion = if (v == null) null else ((v - min) / (max - min)).toFloat().coerceIn(0f, 1f)
    val fuera = v != null && (v < min || v > max)
    BoxWithConstraints(Modifier.fillMaxWidth().height(10.dp)) {
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0xFFC8E6C9)),
        )
        if (fraccion != null) {
            Box(
                Modifier
                    .offset(x = (maxWidth - 10.dp) * fraccion)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (fuera) AgroColors.Error else AgroColors.Primary),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth()) {
        Text(conUnidad(min, s), fontSize = 10.sp, color = AgroColors.TextMuted, modifier = Modifier.weight(1f))
        Text("Rango normal", fontSize = 10.sp, color = AgroColors.TextMuted)
        Text(conUnidad(max, s), fontSize = 10.sp, color = AgroColors.TextMuted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun Mensaje(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    accion: String?,
    onAccion: (() -> Unit)?,
) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(texto, fontSize = 13.sp, color = AgroColors.Title, textAlign = TextAlign.Center)
            if (accion != null && onAccion != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    accion,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AgroColors.Primary)
                        .clickable(onClick = onAccion)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}
