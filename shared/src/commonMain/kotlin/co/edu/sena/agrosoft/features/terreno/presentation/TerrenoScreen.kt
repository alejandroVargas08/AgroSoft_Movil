package co.edu.sena.agrosoft.features.terreno.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.features.terreno.data.Lote
import co.edu.sena.agrosoft.features.terreno.data.Vuelo
import co.edu.sena.agrosoft.features.terreno.data.numero
import com.composables.icons.lucide.*

@Composable
fun TerrenoScreen(vm: TerrenoViewModel, onAbrirVuelo: (Int) -> Unit) {
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F0EA)).padding(4.dp),
        ) {
            PestanaTerreno.entries.forEach { p ->
                val activa = p == e.pestana
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activa) Color.White else Color.Transparent)
                        .clickable { vm.cambiarPestana(p) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        p.etiqueta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (activa) AgroColors.Primary else AgroColors.TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        when {
            e.cargando -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
            e.error != null && e.vuelos.isEmpty() -> Aviso(Lucide.WifiOff, e.error, "Reintentar", vm::cargar)
            e.pestana == PestanaTerreno.VUELOS -> Vuelos(e, vm, onAbrirVuelo)
            else -> Lotes(e, vm)
        }
    }
}

@Composable
private fun Vuelos(e: TerrenoEstado, vm: TerrenoViewModel, onAbrirVuelo: (Int) -> Unit) {
    if (e.loteFiltro != null) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(AgroColors.PrimarySoft)
                .clickable { vm.verLote(null) }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.MapPin, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(e.nombreLote(e.loteFiltro), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AgroColors.Primary)
            Spacer(Modifier.width(6.dp))
            Icon(Lucide.X, contentDescription = "Quitar filtro", tint = AgroColors.Primary, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.height(10.dp))
    }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FiltroVuelos.entries.forEach { f ->
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

    val destacado = e.destacado
    if (destacado != null && e.filtro == FiltroVuelos.TODOS && e.loteFiltro == null) {
        TituloSeccion("Último análisis")
        Destacado(destacado, e) { onAbrirVuelo(destacado.id) }
    }

    TituloSeccion(if (e.loteFiltro != null) "Vuelos del lote" else "Vuelos")
    if (e.visibles.isEmpty()) {
        Aviso(Lucide.Plane, if (e.vuelos.isEmpty()) "Todavía no hay vuelos de dron registrados." else "No hay vuelos en este filtro.", null, null)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            e.visibles.forEach { v -> ItemVuelo(v, e) { onAbrirVuelo(v.id) } }
        }
    }
}

@Composable
private fun Destacado(v: Vuelo, e: TerrenoEstado, onClick: () -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFFE8F0EA))) {
            val portada = e.portada
            if (portada != null && portada.first == v.id) {
                ImagenApi(rutaImagen(v.id, portada.second, Capa.NDVI), "Mapa NDVI de ${v.nombre}", Modifier.fillMaxSize())
            } else {
                Icon(Lucide.Plane, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(32.dp).align(Alignment.Center))
            }
            Text(
                "NDVI · ${v.nombre}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AgroColors.PrimaryDark,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(porcentaje(v.saludable), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AgroColors.Primary)
                Text("saludable", fontSize = 9.sp, color = AgroColors.TextMuted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.nombreLote(v.loteId), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                Text(
                    listOfNotNull(fechaVuelo(v.fechaVuelo), e.nombreDron(v.dronId), "${v.totalCapturas} capturas").joinToString(" · "),
                    fontSize = 11.sp,
                    color = AgroColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Ver resultado",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(AgroColors.Primary).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ItemVuelo(v: Vuelo, e: TerrenoEstado, onClick: () -> Unit) {
    val etiqueta = estadoDe(v)
    Tarjeta(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(AgroColors.PrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Plane, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        v.nombre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroColors.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        etiqueta.texto,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = etiqueta.color,
                        modifier = Modifier.clip(CircleShape).background(etiqueta.fondo).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    listOfNotNull(e.nombreLote(v.loteId), fechaVuelo(v.fechaVuelo), v.saludable?.let { "${porcentaje(it)} saludable" })
                        .joinToString(" · "),
                    fontSize = 11.sp,
                    color = AgroColors.TextMuted,
                )
                if (v.enProceso) {
                    val progreso = (v.ultimoAnalisis?.progreso ?: 0) / 100f
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0xFFE2EAE1))) {
                        Box(Modifier.fillMaxWidth(progreso).fillMaxHeight().clip(CircleShape).background(AgroColors.Info))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${v.ultimoAnalisis?.progreso ?: 0} % · Te avisaremos cuando termine", fontSize = 10.sp, color = AgroColors.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun Lotes(e: TerrenoEstado, vm: TerrenoViewModel) {
    if (e.lotes.isEmpty()) {
        Aviso(Lucide.Map, "No hay lotes para mostrar o no tienes permiso para verlos.", null, null)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        e.lotes.forEach { lote -> ItemLote(lote, e) { vm.verLote(lote.id) } }
    }
}

@Composable
private fun ItemLote(lote: Lote, e: TerrenoEstado, onClick: () -> Unit) {
    val vuelos = e.vuelos.filter { it.loteId == lote.id }
    val salud = vuelos.firstOrNull { it.analizado }?.saludable
    val etiqueta = saludDe(salud)
    Tarjeta(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(AgroColors.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Lucide.Map, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(lote.nombre, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                    Text(
                        listOfNotNull(hectareas(lote.areaHa.numero()), lote.estado?.lowercase()?.replaceFirstChar { it.uppercase() }).joinToString(" · "),
                        fontSize = 11.sp,
                        color = AgroColors.TextMuted,
                    )
                }
                Icon(Lucide.ChevronRight, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AgroColors.SurfaceSoft).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (vuelos.size == 1) "1 vuelo" else "${vuelos.size} vuelos",
                    fontSize = 12.sp,
                    color = AgroColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (salud != null) {
                    Text("${porcentaje(salud)} saludable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = etiqueta.color)
                } else {
                    Text("Sin análisis", fontSize = 12.sp, color = AgroColors.TextMuted)
                }
            }
        }
    }
}

@Composable
internal fun Aviso(icono: ImageVector, texto: String, accion: String?, onAccion: (() -> Unit)?) {
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
