package co.edu.sena.agrosoft.features.inicio.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.core.util.decimal
import com.composables.icons.lucide.*
import androidx.compose.runtime.LaunchedEffect

@Composable
fun InicioAprendizScreen(
    sesionId: Int,
    onAbrirTareas: () -> Unit,
    onAbrirSensores: () -> Unit,
    onAbrirVuelo: () -> Unit,
) {
    val vm = viewModel(key = "inicio-aprendiz-$sesionId") { InicioAprendizViewModel() }
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth()) {
        when {
            e.cargando && e.proxima == null && e.clima == null -> Cargando()
            e.error != null -> ErrorCarga(e.error, vm::cargar)
            else -> {
                TituloSeccion("Tu próxima tarea")
                when {
                    e.proxima != null -> TarjetaProximaTarea(e.proxima, onAbrirTareas)
                    else -> TarjetaSinTareas()
                }

                e.clima?.let { clima ->
                    TituloSeccion("Clima ahora en tu lote")
                    TarjetaClima(clima, onAbrirSensores)
                }

                e.vuelo?.let { vuelo ->
                    TituloSeccion("Último análisis de tu lote")
                    TarjetaVuelo(vuelo, onAbrirVuelo)
                }

                TituloSeccion("Hoy")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Contador(e.pendientes, "Pendientes", AgroColors.Title)
                    Contador(e.porVencer, "Por vencer", AgroColors.Title)
                    Contador(e.devueltas, if (e.devueltas == 1) "Devuelta" else "Devueltas", AgroColors.Error)
                }
            }
        }
    }
}

@Composable
private fun TarjetaProximaTarea(tarea: TareaProxima, onContinuar: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, forma, ambientColor = Color(0x1A1A4D2E), spotColor = Color(0x1A1A4D2E))
            .clip(forma)
            .background(Brush.linearGradient(listOf(AgroColors.Primary.copy(alpha = 0.95f), AgroColors.PrimaryDark)))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(10.dp),
            ) {
                Icon(Lucide.Sprout, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            tarea.etiqueta?.let {
                Text(
                    it,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tarea.devuelta) AgroColors.Error else Color(0xFFA54400),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (tarea.devuelta) AgroColors.ErrorSoft else AgroColors.AccentSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(tarea.titulo, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (tarea.detalle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(tarea.detalle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable(onClick = onContinuar),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (tarea.devuelta) "Corregir tarea" else "Continuar tarea",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AgroColors.PrimaryDark,
            )
        }
    }
}

@Composable
private fun TarjetaSinTareas() {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(AgroColors.PrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Check, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Estás al día", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                Text("No tienes tareas pendientes.", fontSize = 11.sp, color = AgroColors.TextMuted)
            }
        }
    }
}

@Composable
private fun TarjetaClima(clima: ClimaResumen, onClick: () -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(clima.estacion, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                    clima.actualizado?.let {
                        Text("Actualizado ${it.lowercase()}", fontSize = 10.sp, color = Color(0xFF6A7D6D))
                    }
                }
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (clima.conectada) AgroColors.Success else AgroColors.TextSecondary),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (clima.conectada) "Conectada" else "Sin conexión",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (clima.conectada) Color(0xFF047857) else AgroColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Medida(Lucide.Thermometer, AgroColors.Error, clima.temperatura?.let { "${decimal(it)} °C" } ?: "—", "Temperatura")
                Medida(Lucide.Droplets, AgroColors.Info, clima.humedad?.let { "${decimal(it)} %" } ?: "—", "Humedad")
                Medida(
                    Lucide.Sun,
                    AgroColors.Accent,
                    clima.uv?.let { "UV ${decimal(it)}" } ?: "UV —",
                    clima.uv?.let(::nivelUv) ?: "Sin dato",
                )
            }
            if ((clima.uv ?: 0.0) >= 6) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AgroColors.AccentSoft)
                        .padding(12.dp),
                ) {
                    Icon(Lucide.Sun, contentDescription = null, tint = Color(0xFF8B3C05), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "UV ${nivelUv(clima.uv!!).lowercase()}: usa gorra, manga larga y bloqueador antes de salir al campo.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8B3C05),
                    )
                }
            }
            if (clima.enAlerta > 0) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E9DF)))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (clima.enAlerta == 1) "1 sensor requiere atención" else "${clima.enAlerta} sensores requieren atención",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgroColors.Error,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Lucide.ChevronRight, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RowScope.Medida(icono: ImageVector, color: Color, valor: String, etiqueta: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(AgroColors.SurfaceSoft)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(valor, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(etiqueta, fontSize = 9.sp, color = Color(0xFF6A7D6D), maxLines = 1)
    }
}

@Composable
private fun TarjetaVuelo(vuelo: VueloResumen, onClick: () -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier.fillMaxWidth(0.38f).fillMaxHeight().defaultMinSize(minHeight = 122.dp).background(Color(0xFFD7E6D2)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Plane, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.padding(16.dp)) {
                Text("VUELO · ANALIZADO", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = AgroColors.Chip)
                Spacer(Modifier.height(4.dp))
                Text(vuelo.nombre, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                Text("${decimal(vuelo.saludable)} %", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AgroColors.Primary)
                Text("vegetación saludable", fontSize = 10.sp, color = AgroColors.TextMuted)
            }
        }
    }
}

@Composable
private fun RowScope.Contador(valor: Int, etiqueta: String, color: Color) {
    Tarjeta(modifier = Modifier.weight(1f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$valor", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (valor > 0) color else AgroColors.Title)
            Text(etiqueta, fontSize = 10.sp, color = AgroColors.TextMuted)
        }
    }
}

@Composable
private fun Cargando() {
    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun ErrorCarga(mensaje: String, onReintentar: () -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.WifiOff, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(mensaje, fontSize = 13.sp, color = AgroColors.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(
                "Reintentar",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AgroColors.Primary)
                    .clickable(onClick = onReintentar)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

private fun nivelUv(v: Double): String = when {
    v <= 2 -> "Bajo"
    v <= 5 -> "Moderado"
    v <= 7 -> "Alto"
    v <= 10 -> "Muy alto"
    else -> "Extremo"
}
