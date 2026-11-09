package co.edu.sena.agrosoft.features.actividades.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import com.composables.icons.lucide.*

@Composable
fun ActividadesScreen(vm: ActividadesViewModel, onAbrir: (Int) -> Unit) {
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Button(
            onClick = vm::nuevaTarea,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
        ) {
            Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nueva tarea", fontWeight = FontWeight.Bold)
        }

        e.aviso?.let {
            Spacer(Modifier.height(12.dp))
            Franja(it, exito = true)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FiltroActividades.entries.forEach { f -> Chip(f.etiqueta, f == e.filtro) { vm.cambiarFiltro(f) } }
        }

        Spacer(Modifier.height(14.dp))
        when {
            e.cargando -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
            e.error != null && e.items.isEmpty() -> Vacio(Lucide.WifiOff, e.error)
            e.visibles.isEmpty() -> Vacio(
                Lucide.ClipboardCheck,
                if (e.items.isEmpty()) "Aún no hay actividades. Crea la primera con \"Nueva tarea\"." else "No hay actividades en este filtro.",
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                e.visibles.forEach { r -> ItemActividad(r, e.nombreLote(r.actividad.loteId)) { onAbrir(r.actividad.id) } }
            }
        }
    }
}

@Composable
private fun ItemActividad(r: ResumenActividad, lote: String, onClick: () -> Unit) {
    val insignia = insigniaDe(r.situacion)
    Tarjeta(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(AgroColors.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconoTipo(r.actividad.tipo), contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        r.actividad.titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgroColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(lote, r.actividad.tipo.takeIf { it != r.actividad.titulo }).joinToString(" · "),
                        fontSize = 12.sp,
                        color = AgroColors.TextSecondary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Pildora(insignia)
            }

            HorizontalDivider(color = AgroColors.Border, modifier = Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fechaTexto(r.fechaLimite)?.let { "Límite: $it" } ?: fechaTexto(r.actividad.fecha)?.let { "Creada: $it" } ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (r.vencidas > 0) AgroColors.Error else AgroColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (r.total == 0) "Sin aprendices" else "${r.entregadas} de ${r.total} entregadas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AgroColors.Primary,
                )
            }
        }
    }
}

@Composable
internal fun Vacio(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(texto, fontSize = 13.sp, color = AgroColors.Title, textAlign = TextAlign.Center)
        }
    }
}
