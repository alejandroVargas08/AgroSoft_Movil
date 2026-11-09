package co.edu.sena.agrosoft.features.notificaciones.presentation

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
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.notificaciones.data.Notificacion
import com.composables.icons.lucide.*

@Composable
fun NotificacionesScreen(
    vm: NotificacionesViewModel,
    onAbrir: (Notificacion) -> Unit,
) {
    val e = vm.estado
    val nuevas = e.notificaciones.filter { !it.leida }
    val anteriores = e.notificaciones.filter { it.leida }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (e.noLeidas > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (e.noLeidas == 1) "1 sin leer" else "${e.noLeidas} sin leer",
                    fontSize = 12.sp,
                    color = AgroColors.TextMuted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Marcar todas como leídas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AgroColors.Primary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.marcarTodas() }.padding(6.dp),
                )
            }
        }

        when {
            e.cargando && e.notificaciones.isEmpty() -> Cargando()
            e.error != null && e.notificaciones.isEmpty() -> ErrorCarga(e.error, vm::refrescar)
            e.notificaciones.isEmpty() -> Vacio()
            else -> {
                if (nuevas.isNotEmpty()) {
                    TituloSeccion("Nuevas")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        nuevas.forEach { n -> Item(n, vm, onAbrir) }
                    }
                }
                if (anteriores.isNotEmpty()) {
                    TituloSeccion("Anteriores")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        anteriores.forEach { n -> Item(n, vm, onAbrir) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(n: Notificacion, vm: NotificacionesViewModel, onAbrir: (Notificacion) -> Unit) {
    val (icono, color) = iconoDe(n.tipo)
    Tarjeta(
        modifier = Modifier.fillMaxWidth(),
        fondo = if (n.leida) AgroColors.Surface else Color(0xFFF3F8F3),
        borde = if (n.leida) AgroColors.Border else Color(0xFFCFE2D0),
        onClick = {
            vm.marcarLeida(n.id)
            onAbrir(n)
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.titulo,
                        fontSize = 13.sp,
                        fontWeight = if (n.leida) FontWeight.SemiBold else FontWeight.Bold,
                        color = AgroColors.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!n.leida) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(AgroColors.Accent))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(n.mensaje, fontSize = 12.sp, color = AgroColors.Text, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                Fechas.haceCuanto(n.creadoEn)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 10.sp, color = AgroColors.TextMuted)
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).clickable { vm.eliminar(n.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.X, contentDescription = "Eliminar", tint = AgroColors.TextMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun iconoDe(tipo: String?): Pair<ImageVector, Color> = when (tipo) {
    "tarea" -> Lucide.ClipboardCheck to AgroColors.Primary
    "solicitud" -> Lucide.UserPlus to AgroColors.Info
    "insumo_bajo", "entrada", "salida" -> Lucide.Package to AgroColors.Warning
    "herramienta_danada" -> Lucide.Wrench to AgroColors.Error
    else -> Lucide.Bell to AgroColors.TextSecondary
}

@Composable
private fun Vacio() {
    Tarjeta(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.BellOff, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text("No tienes notificaciones.", fontSize = 13.sp, color = AgroColors.TextMuted, textAlign = TextAlign.Center)
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
    Tarjeta(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
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
