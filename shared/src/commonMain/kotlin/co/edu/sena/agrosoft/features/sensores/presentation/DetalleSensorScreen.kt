package co.edu.sena.agrosoft.features.sensores.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.sensores.data.AlertaSensor
import co.edu.sena.agrosoft.features.sensores.data.Sensor
import co.edu.sena.agrosoft.features.sensores.data.aNumero
import com.composables.icons.lucide.*

@Composable
fun DetalleSensorScreen(vm: SensoresViewModel) {
    val e = vm.estado
    val s = e.seleccionado

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (s == null) {
            Mensaje(Lucide.Radio, "Selecciona un sensor de la lista.", null, null)
            return@Column
        }

        val (icono, colorIcono) = iconoDe(s)
        val (textoEstado, colorEstado) = textoSituacion(s.situacion)

        Tarjeta(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${medidaDe(s).nombre.uppercase()} ACTUAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AgroColors.TextMuted,
                        )
                        Text(conUnidad(s.valorActual, s), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                        Text(textoEstado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colorEstado)
                    }
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(colorIcono.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipConexion(s)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Fechas.haceCuanto(s.ultimaMedicion ?: s.ultimaVistaEn) ?: "Sin lecturas",
                        fontSize = 11.sp,
                        color = AgroColors.TextMuted,
                    )
                }
                BarraRango(s)

                Spacer(Modifier.height(16.dp))
                SelectorPeriodo(e.periodo, vm::cambiarPeriodo)

                Spacer(Modifier.height(14.dp))
                when {
                    e.cargandoDetalle -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    }
                    e.errorDetalle != null -> Text(e.errorDetalle, fontSize = 12.sp, color = AgroColors.Error)
                    e.puntos.size < 2 -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (e.lecturas.isEmpty()) "Este sensor todavía no tiene lecturas." else "No hay suficientes lecturas en este periodo.",
                            fontSize = 12.sp,
                            color = AgroColors.TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        Grafica(
                            puntos = e.puntos,
                            minimo = s.minimo,
                            maximo = s.maximo,
                            color = if (colorIcono == AgroColors.Error) AgroColors.Primary else colorIcono,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        val conDia = e.periodo != Periodo.DIA
                        Row(Modifier.fillMaxWidth()) {
                            Text(fechaCorta(e.puntos.first().first, conDia), fontSize = 9.sp, color = AgroColors.TextMuted, modifier = Modifier.weight(1f))
                            Text(fechaCorta(e.puntos.last().first, conDia), fontSize = 9.sp, color = AgroColors.TextMuted)
                        }
                        if (s.minimo != null || s.maximo != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Leyenda(AgroColors.Error, "Máximo")
                                Spacer(Modifier.width(14.dp))
                                Leyenda(AgroColors.Warning, "Mínimo")
                            }
                        }
                    }
                }
            }
        }

        if (e.puntos.isNotEmpty()) {
            val valores = e.puntos.map { it.second }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Estadistica(conUnidad(valores.average(), s), "Promedio", Modifier.weight(1f))
                Estadistica(conUnidad(valores.min(), s), "Mínimo", Modifier.weight(1f))
                Estadistica(conUnidad(valores.max(), s), "Máximo", Modifier.weight(1f))
                Estadistica(e.alertasDelPeriodo.size.toString(), "Alertas", Modifier.weight(1f))
            }
        }

        TituloSeccion("Alertas recientes")
        val recientes = e.alertas.sortedByDescending { Fechas.instante(it.fechaAlerta)?.toEpochMilliseconds() ?: 0L }.take(5)
        if (recientes.isEmpty()) {
            Mensaje(Lucide.CircleCheck, "Este sensor no ha registrado alertas.", null, null)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recientes.forEach { ItemAlerta(it, s) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Los rangos y la configuración del sensor se editan desde la web.",
            fontSize = 11.sp,
            color = AgroColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectorPeriodo(actual: Periodo, onCambiar: (Periodo) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEF4EC)).padding(4.dp),
    ) {
        Periodo.entries.forEach { p ->
            val activo = p == actual
            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (activo) Color.White else Color.Transparent)
                    .clickable { onCambiar(p) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    p.etiqueta,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activo) AgroColors.Primary else AgroColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun Grafica(
    puntos: List<Pair<Long, Double>>,
    minimo: Double?,
    maximo: Double?,
    color: Color,
    modifier: Modifier,
) {
    val paso = (puntos.size / 150).coerceAtLeast(1)
    val datos = puntos.filterIndexed { i, _ -> i % paso == 0 || i == puntos.lastIndex }
    val rejilla = AgroColors.Border
    val colorMax = AgroColors.Error
    val colorMin = AgroColors.Warning

    Canvas(modifier) {
        val valores = datos.map { it.second } + listOfNotNull(minimo, maximo)
        var bajo = valores.min()
        var alto = valores.max()
        if (alto - bajo < 0.0001) {
            alto += 1
            bajo -= 1
        }
        val margen = (alto - bajo) * 0.12
        bajo -= margen
        alto += margen

        val t0 = datos.first().first
        val t1 = datos.last().first
        val ancho = size.width
        val altura = size.height
        fun x(t: Long): Float = if (t1 == t0) ancho / 2 else ((t - t0).toFloat() / (t1 - t0).toFloat()) * ancho
        fun y(v: Double): Float = (altura - ((v - bajo) / (alto - bajo)) * altura).toFloat()

        for (i in 0..3) {
            val yy = altura * i / 3f
            drawLine(rejilla, Offset(0f, yy), Offset(ancho, yy), strokeWidth = 1f)
        }

        val guiones = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
        maximo?.let { drawLine(colorMax, Offset(0f, y(it)), Offset(ancho, y(it)), strokeWidth = 2f, pathEffect = guiones) }
        minimo?.let { drawLine(colorMin, Offset(0f, y(it)), Offset(ancho, y(it)), strokeWidth = 2f, pathEffect = guiones) }

        val linea = Path()
        datos.forEachIndexed { i, (t, v) ->
            if (i == 0) linea.moveTo(x(t), y(v)) else linea.lineTo(x(t), y(v))
        }
        val area = Path().apply {
            addPath(linea)
            lineTo(x(t1), altura)
            lineTo(x(t0), altura)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(linea, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val (tu, vu) = datos.last()
        drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(x(tu), y(vu)))
        drawCircle(color, radius = 4.dp.toPx(), center = Offset(x(tu), y(vu)))
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(14.dp).height(2.dp).background(color))
        Spacer(Modifier.width(5.dp))
        Text(texto, fontSize = 10.sp, color = AgroColors.TextMuted)
    }
}

@Composable
private fun Estadistica(valor: String, etiqueta: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(etiqueta, fontSize = 9.sp, color = AgroColors.TextMuted)
    }
}

@Composable
private fun ItemAlerta(a: AlertaSensor, s: Sensor) {
    val alto = a.tipo == "MAXIMO"
    val color = if (alto) AgroColors.Error else AgroColors.Warning
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (alto) Lucide.ArrowUp else Lucide.ArrowDown, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (alto) "Superó el máximo" else "Bajó del mínimo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroColors.Title,
                )
                Text(
                    "Valor ${conUnidad(a.valor.aNumero(), s)} · límite ${conUnidad(a.umbral.aNumero(), s)}",
                    fontSize = 11.sp,
                    color = AgroColors.TextSecondary,
                )
            }
            Text(Fechas.haceCuanto(a.fechaAlerta) ?: "", fontSize = 10.sp, color = AgroColors.TextMuted)
        }
    }
}
