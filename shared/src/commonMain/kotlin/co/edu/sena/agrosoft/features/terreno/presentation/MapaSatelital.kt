package co.edu.sena.agrosoft.features.terreno.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.features.terreno.data.Punto
import co.edu.sena.agrosoft.features.terreno.data.centroide
import coil3.compose.AsyncImage
import com.composables.icons.lucide.LocateFixed
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

private const val URL_TESELAS = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile"
private const val ZOOM_MAXIMO_TESELAS = 19
private const val ZOOM_MINIMO = 3.0
private const val ZOOM_MAXIMO = 21.0
private const val ZOOM_SIN_FIGURAS = 15.0

data class FiguraMapa(
    val id: Int,
    val vertices: List<Punto>,
    val color: Color,
    val etiqueta: String? = null,
    val resaltada: Boolean = false,
    val relleno: Float = 0.18f,
)

private fun mundoX(lng: Double): Double = (lng + 180.0) / 360.0

private fun mundoY(lat: Double): Double {
    val rad = lat.coerceIn(-85.0, 85.0) * PI / 180.0
    return (1 - ln(tan(rad) + 1 / cos(rad)) / PI) / 2
}

private fun lngDe(x: Double): Double = x * 360.0 - 180.0

private fun latDe(y: Double): Double = atan(sinh(PI * (1 - 2 * y))) * 180.0 / PI

fun contiene(vertices: List<Punto>, p: Punto): Boolean {
    var dentro = false
    var j = vertices.lastIndex
    for (i in vertices.indices) {
        val a = vertices[i]
        val b = vertices[j]
        if ((a.lat > p.lat) != (b.lat > p.lat) && p.lng < (b.lng - a.lng) * (p.lat - a.lat) / (b.lat - a.lat) + a.lng) dentro = !dentro
        j = i
    }
    return dentro
}

@Stable
class EstadoMapa {
    var zoom by mutableStateOf(ZOOM_SIN_FIGURAS)
        private set
    var centroX by mutableStateOf(0.5)
        private set
    var centroY by mutableStateOf(0.5)
        private set
    var anchoPx by mutableStateOf(0f)
        internal set
    var altoPx by mutableStateOf(0f)
        internal set
    var teselaPx by mutableStateOf(256f)
        internal set
    var listo by mutableStateOf(false)
        private set

    private val escala: Double get() = teselaPx * 2.0.pow(zoom)

    fun aPantalla(p: Punto): Offset = Offset(
        ((mundoX(p.lng) - centroX) * escala + anchoPx / 2).toFloat(),
        ((mundoY(p.lat) - centroY) * escala + altoPx / 2).toFloat(),
    )

    fun aGeo(o: Offset): Punto {
        val x = centroX + (o.x - anchoPx / 2) / escala
        val y = centroY + (o.y - altoPx / 2) / escala
        return Punto(latDe(y), lngDe(x))
    }

    fun ajustar(puntos: List<Punto>, margenPx: Float = 40f) {
        if (anchoPx <= 0f || altoPx <= 0f) return
        if (puntos.isEmpty()) {
            listo = true
            return
        }
        val xs = puntos.map { mundoX(it.lng) }
        val ys = puntos.map { mundoY(it.lat) }
        val ancho = (xs.max() - xs.min()).coerceAtLeast(1e-9)
        val alto = (ys.max() - ys.min()).coerceAtLeast(1e-9)
        centroX = (xs.min() + xs.max()) / 2
        centroY = (ys.min() + ys.max()) / 2
        val util = min((anchoPx - 2 * margenPx) / (ancho * teselaPx), (altoPx - 2 * margenPx) / (alto * teselaPx))
        zoom = log2(util).coerceIn(ZOOM_MINIMO, ZOOM_MAXIMO_TESELAS.toDouble())
        listo = true
    }

    fun mover(desplazamiento: Offset) {
        centroX -= desplazamiento.x / escala
        centroY -= desplazamiento.y / escala
    }

    fun escalar(factor: Float, foco: Offset) {
        val antes = escala
        val wx = centroX + (foco.x - anchoPx / 2) / antes
        val wy = centroY + (foco.y - altoPx / 2) / antes
        zoom = (zoom + log2(factor.toDouble())).coerceIn(ZOOM_MINIMO, ZOOM_MAXIMO)
        centroX = wx - (foco.x - anchoPx / 2) / escala
        centroY = wy - (foco.y - altoPx / 2) / escala
    }
}

@Composable
fun rememberEstadoMapa(): EstadoMapa = remember { EstadoMapa() }

@Composable
fun MapaSatelital(
    figuras: List<FiguraMapa>,
    modifier: Modifier = Modifier,
    estado: EstadoMapa = rememberEstadoMapa(),
    encuadre: List<Punto>? = null,
    onToqueFigura: ((Int?) -> Unit)? = null,
) {
    val densidad = LocalDensity.current
    val medidor = rememberTextMeasurer()
    val puntos = remember(figuras) { figuras.flatMap { it.vertices } }
    val objetivo = encuadre?.takeIf { it.isNotEmpty() } ?: puntos

    BoxWithConstraints(
        modifier
            .clipToBounds()
            .background(Color(0xFF1F2A22))
            .pointerInput(Unit) {
                detectTransformGestures { foco, desplazamiento, factor, _ ->
                    if (factor != 1f) estado.escalar(factor, foco)
                    estado.mover(desplazamiento)
                }
            }
            .pointerInput(figuras, onToqueFigura) {
                detectTapGestures(
                    onDoubleTap = { estado.escalar(2f, it) },
                    onTap = { toque ->
                        val p = estado.aGeo(toque)
                        onToqueFigura?.invoke(figuras.lastOrNull { it.vertices.size >= 3 && contiene(it.vertices, p) }?.id)
                    },
                )
            },
    ) {
        val ancho = constraints.maxWidth.toFloat()
        val alto = constraints.maxHeight.toFloat()

        LaunchedEffect(ancho, alto, objetivo) {
            estado.anchoPx = ancho
            estado.altoPx = alto
            estado.teselaPx = 256f * (densidad.density / 2f).coerceAtLeast(1f)
            estado.ajustar(objetivo)
        }

        if (estado.listo) {
            Teselas(estado, ancho, alto)

            Canvas(Modifier.matchParentSize()) {
                figuras.filter { it.vertices.size >= 3 }.sortedBy { it.resaltada }.forEach { f ->
                    val camino = Path().apply {
                        f.vertices.forEachIndexed { i, v ->
                            val o = estado.aPantalla(v)
                            if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                        }
                        close()
                    }
                    drawPath(camino, f.color.copy(alpha = if (f.resaltada) 0.35f else f.relleno))
                    drawPath(
                        camino,
                        if (f.resaltada) Color(0xFFFBBF24) else f.color,
                        style = Stroke(width = (if (f.resaltada) 3.dp else 2.dp).toPx()),
                    )
                }
                figuras.filter { !it.etiqueta.isNullOrBlank() && it.vertices.size >= 3 }.forEach { f ->
                    val centro = estado.aPantalla(centroide(f.vertices))
                    val texto = medidor.measure(
                        f.etiqueta!!,
                        TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    val w = texto.size.width.toFloat()
                    val h = texto.size.height.toFloat()
                    val relleno = 6.dp.toPx()
                    drawRoundRect(
                        Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(centro.x - w / 2 - relleno, centro.y - h / 2 - relleno / 2),
                        size = Size(w + relleno * 2, h + relleno),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )
                    drawText(texto, topLeft = Offset(centro.x - w / 2, centro.y - h / 2))
                }
            }
        }

        Column(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            BotonMapa(Lucide.Plus, "Acercar") { estado.escalar(2f, Offset(ancho / 2, alto / 2)) }
            BotonMapa(Lucide.Minus, "Alejar") { estado.escalar(0.5f, Offset(ancho / 2, alto / 2)) }
            BotonMapa(Lucide.LocateFixed, "Centrar") { estado.ajustar(objetivo) }
        }

        Text(
            "Imágenes © Esri, Maxar",
            fontSize = 8.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun Teselas(estado: EstadoMapa, ancho: Float, alto: Float) {
    val densidad = LocalDensity.current
    val nivel = floor(estado.zoom).toInt().coerceIn(0, ZOOM_MAXIMO_TESELAS)
    val n = 1 shl nivel
    val lado = estado.teselaPx * 2.0.pow(estado.zoom - nivel)
    val origenX = estado.centroX * n * lado - ancho / 2
    val origenY = estado.centroY * n * lado - alto / 2
    val x0 = floor(origenX / lado).toInt()
    val x1 = floor((origenX + ancho) / lado).toInt()
    val y0 = floor(origenY / lado).toInt().coerceAtLeast(0)
    val y1 = floor((origenY + alto) / lado).toInt().coerceAtMost(n - 1)
    val tamano = with(densidad) { (lado.toFloat() + 1f).toDp() }

    for (ty in y0..y1) {
        for (tx in x0..x1) {
            key(nivel, tx, ty) {
                val columna = ((tx % n) + n) % n
                AsyncImage(
                    model = "$URL_TESELAS/$nivel/$ty/$columna",
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset((tx * lado - origenX).roundToInt(), (ty * lado - origenY).roundToInt()) }
                        .size(tamano),
                )
            }
        }
    }
}

@Composable
private fun BotonMapa(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(bottom = 6.dp)
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = descripcion, tint = Color(0xFF1A4D2E), modifier = Modifier.size(16.dp))
    }
}
