package co.edu.sena.agrosoft.features.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.features.sensores.data.Sensor
import co.edu.sena.agrosoft.features.sensores.presentation.Medida
import co.edu.sena.agrosoft.features.sensores.presentation.conUnidad
import co.edu.sena.agrosoft.features.sensores.presentation.medidaDe
import com.composables.icons.lucide.*

@Composable
fun InicioAdminScreen(
    vm: AdminViewModel,
    onSolicitudes: () -> Unit,
    onAlertas: () -> Unit,
    onSensores: () -> Unit,
) {
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (e.cargando) {
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
            return@Column
        }
        e.error?.let { MensajeAdmin(Lucide.WifiOff, it) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(e.activos.toString(), "Usuarios activos", Lucide.Users, Modifier.weight(1f))
            Kpi(
                e.solicitudes.size.toString(),
                "Solicitudes",
                Lucide.ShieldCheck,
                Modifier.weight(1f),
                resaltado = e.solicitudes.isNotEmpty(),
                onClick = onSolicitudes,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(e.fichasActivas.toString(), "Fichas activas", Lucide.FileText, Modifier.weight(1f))
            Kpi(
                e.sensoresEnAlerta.size.toString(),
                "Estaciones en alerta",
                Lucide.Radio,
                Modifier.weight(1f),
                alerta = e.sensoresEnAlerta.isNotEmpty(),
                onClick = onAlertas,
            )
        }

        TituloSeccion("Clima de la finca")
        Clima(e.sensores, e.sensoresEnAlerta.size, onSensores)

        val fichas = e.fichas.filter { it.activa }.sortedByDescending { it.totalAprendices }.take(5)
        if (fichas.isNotEmpty()) {
            TituloSeccion("Fichas")
            Tarjeta(modifier = Modifier.fillMaxWidth()) {
                fichas.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = AgroColors.Border, modifier = Modifier.padding(horizontal = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AgroColors.PrimarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Lucide.GraduationCap, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(f.numero?.let { "Ficha $it" }, f.tipo).joinToString(" · "),
                                fontSize = 11.sp,
                                color = AgroColors.TextSecondary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${f.totalAprendices}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                            Text(
                                if (f.instructores.size == 1) "1 instructor" else "${f.instructores.size} instructores",
                                fontSize = 10.sp,
                                color = AgroColors.TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Kpi(
    valor: String,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier,
    resaltado: Boolean = false,
    alerta: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val forma = RoundedCornerShape(20.dp)
    val color = when {
        resaltado -> AgroColors.Accent
        alerta -> AgroColors.Error
        else -> AgroColors.Primary
    }
    Column(
        modifier
            .clip(forma)
            .background(if (resaltado) AgroColors.AccentSoft else Color.White)
            .border(1.dp, if (resaltado) Color(0xFFFED7AA) else AgroColors.BorderCard, forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(12.dp))
        Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (alerta) AgroColors.Error else AgroColors.Title)
        Text(etiqueta, fontSize = 11.sp, color = AgroColors.TextMuted)
    }
}

@Composable
private fun Clima(sensores: List<Sensor>, enAlerta: Int, onClick: () -> Unit) {
    val conectados = sensores.filter { it.activo && it.conectado && it.valorActual != null }
    fun promedio(m: Medida): String {
        val grupo = conectados.filter { medidaDe(it) == m }
        if (grupo.isEmpty()) return "—"
        return conUnidad(grupo.mapNotNull { it.valorActual }.average(), grupo.first())
    }
    Tarjeta(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Medicion(Lucide.Thermometer, AgroColors.Error, promedio(Medida.TEMPERATURA), "Temperatura", Modifier.weight(1f))
                Medicion(Lucide.Droplets, AgroColors.Info, promedio(Medida.HUMEDAD), "Humedad", Modifier.weight(1f))
                Medicion(Lucide.Sun, AgroColors.Accent, promedio(Medida.UV), "Índice UV", Modifier.weight(1f))
            }
            HorizontalDivider(color = AgroColors.Border, modifier = Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        sensores.isEmpty() -> "Sin estaciones registradas"
                        enAlerta == 0 -> "Todas las estaciones están bien"
                        enAlerta == 1 -> "1 estación requiere atención"
                        else -> "$enAlerta estaciones requieren atención"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enAlerta > 0) AgroColors.Error else AgroColors.Primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(Lucide.ChevronRight, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun Medicion(icono: ImageVector, color: Color, valor: String, etiqueta: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(AgroColors.SurfaceSoft).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(valor, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title, maxLines = 1)
        Text(etiqueta, fontSize = 9.sp, color = AgroColors.TextMuted)
    }
}

@Composable
internal fun MensajeAdmin(icono: ImageVector, texto: String) {
    Tarjeta(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(texto, fontSize = 13.sp, color = AgroColors.Title, textAlign = TextAlign.Center)
        }
    }
}
