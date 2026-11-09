package co.edu.sena.agrosoft.features.terreno.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.features.terreno.data.Analisis
import co.edu.sena.agrosoft.features.terreno.data.Vuelo
import co.edu.sena.agrosoft.features.terreno.data.numero
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay

@Composable
fun DetalleVueloScreen(vm: TerrenoViewModel, puedeAnalizar: Boolean) {
    val e = vm.estado
    val v = e.seleccionado

    LaunchedEffect(Unit) {
        if (e.seleccionadoId == null) vm.abrirUltimo()
    }

    val enCurso = e.analisis?.estado in setOf("PENDIENTE", "PROCESANDO")
    LaunchedEffect(enCurso, e.seleccionadoId) {
        while (enCurso) {
            delay(5_000)
            vm.cargarDetalle(silencioso = true)
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (v == null) {
            if (e.errorDetalle != null) {
                Aviso(Lucide.Plane, e.errorDetalle, null, null)
            } else {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
            }
            return@Column
        }

        Visor(v, e, vm)

        if (e.capturas.size > 1) {
            Spacer(Modifier.height(10.dp))
            Miniaturas(v, e, vm)
        }

        Spacer(Modifier.height(12.dp))
        DatosVuelo(v, e)

        e.aviso?.let {
            Spacer(Modifier.height(12.dp))
            Franja(it, AgroColors.SuccessSoft, AgroColors.Primary)
        }
        e.errorDetalle?.let {
            Spacer(Modifier.height(12.dp))
            Franja(it, AgroColors.ErrorSoft, AgroColors.Error)
        }

        val a = e.analisis
        when {
            e.cargandoDetalle && a == null -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            }
            a == null -> {
                TituloSeccion("Análisis")
                Aviso(Lucide.ScanSearch, "Este vuelo todavía no tiene análisis.", null, null)
            }
            a.estado == "PENDIENTE" || a.estado == "PROCESANDO" -> EnProceso(a)
            a.estado == "ERROR" -> {
                TituloSeccion("Análisis")
                Franja(a.mensajeError ?: "El análisis terminó con error.", AgroColors.ErrorSoft, AgroColors.Error)
            }
            else -> Resultados(a)
        }

        if (puedeAnalizar && v.puedeAnalizarse) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = vm::solicitarAnalisis,
                enabled = !e.solicitando,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
            ) {
                if (e.solicitando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Lucide.Sparkles, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (a == null) "Analizar vuelo" else "Volver a analizar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Visor(v: Vuelo, e: TerrenoEstado, vm: TerrenoViewModel) {
    val captura = e.captura
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFCADDC6)),
    ) {
        if (captura != null) {
            ImagenApi(rutaImagen(v.id, captura.id, e.capa), "Captura ${captura.numeroCaptura} · ${e.capa.etiqueta}", Modifier.fillMaxSize())
        } else if (!e.cargandoDetalle) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Lucide.ImageOff, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text("Este vuelo no tiene imágenes", fontSize = 12.sp, color = AgroColors.TextMuted)
            }
        }

        if (captura != null) {
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Capa.entries.forEach { c ->
                    val activa = c == e.capa
                    Text(
                        c.etiqueta,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activa) Color.White else AgroColors.PrimaryDark,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (activa) AgroColors.Primary.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.85f))
                            .clickable { vm.cambiarCapa(c) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            if (e.capa != Capa.RGB) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.88f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Estrés alto", fontSize = 9.sp, color = AgroColors.Text)
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFF59E0B), Color(0xFF10B981)))),
                    )
                    Text("Saludable", fontSize = 9.sp, color = AgroColors.Text)
                }
            }
        }
    }
}

@Composable
private fun Miniaturas(v: Vuelo, e: TerrenoEstado, vm: TerrenoViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        e.capturas.take(40).forEachIndexed { i, c ->
            val activa = i == e.capturaActual
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(if (activa) 3.dp else 1.dp, if (activa) AgroColors.Primary else AgroColors.Border, RoundedCornerShape(12.dp))
                    .clickable { vm.cambiarCaptura(i) },
            ) {
                ImagenApi(rutaImagen(v.id, c.id, Capa.RGB, completa = false), "Captura ${c.numeroCaptura}", Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun DatosVuelo(v: Vuelo, e: TerrenoEstado) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Dato(Lucide.MapPin, e.nombreLote(v.loteId))
            Dato(Lucide.Calendar, fechaVuelo(v.fechaVuelo))
            e.nombreDron(v.dronId)?.let { Dato(Lucide.Plane, it) }
            Dato(
                Lucide.Images,
                buildString {
                    append(if (v.totalCapturas == 1) "1 captura" else "${v.totalCapturas} capturas")
                    v.alturaVueloM.numero()?.let { append(" · ${it.toInt()} m de altura") }
                },
            )
            v.observaciones?.takeIf { it.isNotBlank() }?.let { Dato(Lucide.StickyNote, it) }
        }
    }
}

@Composable
private fun Dato(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, fontSize = 12.sp, color = AgroColors.Text)
    }
}

@Composable
private fun EnProceso(a: Analisis) {
    TituloSeccion("Análisis en proceso")
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (a.etapa) {
                        "INTERPRETACION_IA" -> "Interpretando con IA"
                        "INDICES" -> "Calculando índices de vegetación"
                        else -> "En cola"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroColors.Title,
                    modifier = Modifier.weight(1f),
                )
                Text("${a.progreso} %", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Info)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFE2EAE1))) {
                Box(Modifier.fillMaxWidth(a.progreso / 100f).fillMaxHeight().clip(CircleShape).background(AgroColors.Info))
            }
            Spacer(Modifier.height(8.dp))
            Text("Esta pantalla se actualiza sola. También te llegará una notificación.", fontSize = 11.sp, color = AgroColors.TextMuted)
        }
    }
}

@Composable
private fun Resultados(a: Analisis) {
    val r = a.resumen ?: return
    val saludable = r.distribucionNdvi["SALUDABLE"]
    val salud = saludDe(saludable)

    Spacer(Modifier.height(12.dp))
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SALUD GENERAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = AgroColors.TextMuted)
                    Text(porcentaje(saludable), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AgroColors.Primary)
                    Text("de vegetación saludable", fontSize = 11.sp, color = AgroColors.TextMuted)
                }
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(salud.fondo).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Lucide.Leaf, contentDescription = null, tint = salud.color, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(salud.texto, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = salud.color)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
                CLASES_NDVI.forEach { (clave, _, color) ->
                    val pct = (r.distribucionNdvi[clave] ?: 0.0).toFloat()
                    if (pct > 0f) Box(Modifier.weight(pct).fillMaxHeight().background(color))
                }
            }
            Spacer(Modifier.height(10.dp))
            CLASES_NDVI.forEach { (clave, nombre, color) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(nombre, fontSize = 12.sp, color = AgroColors.TextSecondary, modifier = Modifier.weight(1f))
                    Text(porcentaje(r.distribucionNdvi[clave]), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
                }
            }
        }
    }

    TituloSeccion("Índices promedio")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("NDVI" to r.ndvi, "NDRE" to r.ndre, "GNDVI" to r.gndvi, "NDWI" to r.ndwi).forEach { (nombre, est) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(est?.let { indice(it.promedio) } ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                Text(nombre, fontSize = 9.sp, color = AgroColors.TextMuted)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "${r.capturasAnalizadas} capturas analizadas · ${r.capturasCriticas} críticas",
        fontSize = 11.sp,
        color = if (r.capturasCriticas > 0) AgroColors.Error else AgroColors.TextMuted,
    )

    val sintesis = r.ia?.sintesis
    if (sintesis != null) {
        TituloSeccion("Interpretación de la IA")
        Tarjeta(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Lucide.Sparkles, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(sintesis.resumen ?: "Resumen del vuelo", fontSize = 13.sp, color = AgroColors.Text, lineHeight = 19.sp)
                }
                if (sintesis.hallazgosPrincipales.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("Hallazgos principales", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                    sintesis.hallazgosPrincipales.forEachIndexed { i, h ->
                        Text("${i + 1}. $h", fontSize = 12.sp, color = AgroColors.TextSecondary, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                if (sintesis.recomendaciones.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AgroColors.AccentSoft).padding(12.dp),
                    ) {
                        Text("Recomendaciones", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B3C05))
                        sintesis.recomendaciones.forEach {
                            Text("• $it", fontSize = 12.sp, color = Color(0xFF8B3C05), lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                sintesis.zonasCandidatasSiembra?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Zonas para sembrar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                    Text(it, fontSize = 12.sp, color = AgroColors.TextSecondary, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (sintesis.advertencias.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    sintesis.advertencias.forEach {
                        Text("⚠ $it", fontSize = 11.sp, color = AgroColors.Warning, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    } else {
        r.ia?.mensaje?.let {
            Spacer(Modifier.height(10.dp))
            Franja(it, AgroColors.WarningSoft, AgroColors.Warning)
        }
    }
}

@Composable
private fun Franja(texto: String, fondo: Color, color: Color) {
    Text(
        texto,
        fontSize = 12.sp,
        color = color,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fondo).padding(12.dp),
    )
}
