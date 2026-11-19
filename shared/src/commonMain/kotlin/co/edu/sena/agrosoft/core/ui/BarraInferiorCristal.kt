package co.edu.sena.agrosoft.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.navigation.Pantalla
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.theme.AgroColors
import com.composables.icons.lucide.*
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.glass.*
import kotlin.math.roundToInt

private data class PestanaCristal(val pantalla: Pantalla, val etiqueta: String, val icono: ImageVector)

private fun pestanasCristalDe(rol: Rol): List<PestanaCristal> = listOf(
    PestanaCristal(Pantalla.INICIO, "Inicio", Lucide.House),
    when (rol) {
        Rol.APRENDIZ -> PestanaCristal(Pantalla.TRABAJO, "Tareas", Lucide.ClipboardCheck)
        Rol.INSTRUCTOR -> PestanaCristal(Pantalla.TRABAJO, "Revisar", Lucide.ClipboardCheck)
        Rol.ADMINISTRADOR -> PestanaCristal(Pantalla.TRABAJO, "Solicitudes", Lucide.Users)
    },
    PestanaCristal(Pantalla.AGROBOT, "AgroBot", Lucide.Bot),
    if (rol == Rol.ADMINISTRADOR) PestanaCristal(Pantalla.ALERTAS, "Alertas", Lucide.TriangleAlert)
    else PestanaCristal(Pantalla.TERRENO, "Terreno", Lucide.Map),
    PestanaCristal(Pantalla.MAS, "Más", Lucide.Menu),
)

@Composable
fun BarraInferiorCristal(
    rol: Rol,
    actual: Pantalla,
    insignias: Map<Pantalla, Int>,
    hazeState: HazeState,
    onCambiar: (Pantalla) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pestanas = remember(rol) { pestanasCristalDe(rol) }
    val indice = pestanas.indexOfFirst { it.pantalla == actual }.coerceAtLeast(0)
    val forma = RoundedCornerShape(34.dp)
    val formaBurbuja = RoundedCornerShape(28.dp)
    val haptica = LocalHapticFeedback.current
    val actualVigente by rememberUpdatedState(actual)
    val cambiar by rememberUpdatedState(onCambiar)

    var arrastreX by remember { mutableStateOf<Float?>(null) }
    var presionada by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(
        modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 14.dp,
                shape = forma,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .hazeGlass(
                input = HazeInput.Backdrop(hazeState),
                style = GlassStyle {
                    tint(Color.White.copy(alpha = 0.55f))
                    shape(forma)
                    surfaceProfile(SurfaceProfile.Squircle)
                    specularIntensity(0.2f)
                    ambientResponse(0.1f)
                    edgeSoftness(4.dp)
                },
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.4f)),
                ),
                shape = forma,
            )
            .padding(6.dp),
    ) {
        val anchoPx = constraints.maxWidth.toFloat() / pestanas.size
        val ancho = maxWidth / pestanas.size
        val maximoPx = anchoPx * pestanas.lastIndex

        val indiceArrastre = arrastreX?.let { (it / anchoPx).toInt().coerceIn(0, pestanas.lastIndex) }
        val indiceVisual = indiceArrastre ?: presionada ?: indice
        val interactuando = arrastreX != null || presionada != null

        LaunchedEffect(indiceArrastre) {
            if (indiceArrastre != null) haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        val destinoPx = when {
            arrastreX != null -> (arrastreX!! - anchoPx / 2).coerceIn(0f, maximoPx)
            presionada != null -> anchoPx * presionada!!
            else -> anchoPx * indice
        }
        val posicion by animateFloatAsState(
            targetValue = destinoPx,
            animationSpec = if (arrastreX != null) spring(stiffness = Spring.StiffnessHigh)
            else spring(dampingRatio = 0.68f, stiffness = 420f),
            label = "burbuja",
        )
        val escala by animateFloatAsState(
            targetValue = if (interactuando) 1.12f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
            label = "escala",
        )
        val fondoBurbuja by animateColorAsState(
            if (interactuando) Color.White.copy(alpha = 0.6f) else AgroColors.Primary.copy(alpha = 0.13f),
            label = "fondo",
        )

        Box(
            Modifier
                .offset { IntOffset(posicion.roundToInt(), 0) }
                .width(ancho)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                }
                .shadow(if (interactuando) 8.dp else 0.dp, formaBurbuja, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.12f))
                .clip(formaBurbuja)
                .background(fondoBurbuja)
                .border(1.dp, Color.White.copy(alpha = if (interactuando) 0.95f else 0.55f), formaBurbuja),
        )

        Row(
            Modifier
                .fillMaxSize()
                .pointerInput(pestanas) {
                    detectHorizontalDragGestures(
                        onDragStart = { inicio -> arrastreX = inicio.x },
                        onDragEnd = {
                            val ancho = size.width.toFloat() / pestanas.size
                            val destino = ((arrastreX ?: 0f) / ancho).toInt().coerceIn(0, pestanas.lastIndex)
                            arrastreX = null
                            if (pestanas[destino].pantalla != actualVigente) cambiar(pestanas[destino].pantalla)
                        },
                        onDragCancel = { arrastreX = null },
                        onHorizontalDrag = { cambio, delta ->
                            cambio.consume()
                            arrastreX = ((arrastreX ?: cambio.position.x) + delta).coerceIn(0f, size.width.toFloat())
                        },
                    )
                },
        ) {
            pestanas.forEachIndexed { i, p ->
                ItemCristal(
                    pestana = p,
                    resaltada = i == indiceVisual,
                    presionado = presionada == i,
                    insignia = insignias[p.pantalla] ?: 0,
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(p.pantalla) {
                            detectTapGestures(
                                onPress = {
                                    presionada = i
                                    tryAwaitRelease()
                                    presionada = null
                                },
                                onTap = {
                                    haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (p.pantalla != actualVigente) cambiar(p.pantalla)
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun ItemCristal(
    pestana: PestanaCristal,
    resaltada: Boolean,
    presionado: Boolean,
    insignia: Int,
    modifier: Modifier,
) {
    val color by animateColorAsState(
        if (resaltada) AgroColors.Primary else Color(0xFF374151),
        label = "color",
    )
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "presion",
    )
    Column(
        modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(pestana.icono, contentDescription = pestana.etiqueta, tint = color, modifier = Modifier.size(22.dp))
            if (insignia > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-6).dp)
                        .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                        .clip(CircleShape)
                        .background(AgroColors.Accent)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (insignia > 9) "9+" else insignia.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            pestana.etiqueta,
            fontSize = 10.sp,
            fontWeight = if (resaltada) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
        )
    }
}
