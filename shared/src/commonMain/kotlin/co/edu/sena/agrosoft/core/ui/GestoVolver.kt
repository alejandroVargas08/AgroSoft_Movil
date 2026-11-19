package co.edu.sena.agrosoft.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch

class GestoVolver internal constructor(private val animable: Animatable<Float, AnimationVector1D>) {
    val progreso: Float get() = animable.value
}

@Composable
fun rememberGestoVolver(onVolver: (() -> Unit)?): GestoVolver {
    val volver by rememberUpdatedState(onVolver)
    val animable = remember { Animatable(0f) }
    val alcance = rememberCoroutineScope()
    val estado = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    val enCurso = estado.transitionState as? NavigationEventTransitionState.InProgress
    val progresoDedo = enCurso?.latestEvent?.progress

    LaunchedEffect(progresoDedo) {
        if (progresoDedo != null) animable.snapTo(progresoDedo)
    }

    NavigationBackHandler(
        state = estado,
        isBackEnabled = onVolver != null,
        onBackCancelled = {
            alcance.launch { animable.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 400f)) }
        },
        onBackCompleted = {
            alcance.launch {
                animable.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
                volver?.invoke()
                animable.snapTo(0f)
            }
        },
    )

    return remember(animable) { GestoVolver(animable) }
}

fun Modifier.sombraIzquierda(progreso: Float): Modifier = drawWithContent {
    if (progreso > 0f) {
        val ancho = 16.dp.toPx()
        val alfa = (0.18f * (1f - progreso)).coerceIn(0f, 0.18f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = alfa)),
                startX = -ancho,
                endX = 0f,
            ),
            topLeft = Offset(-ancho, 0f),
            size = Size(ancho, size.height),
        )
    }
    drawContent()
}

@Composable
fun ContenedorVolver(
    onVolver: (() -> Unit)?,
    fondo: (@Composable () -> Unit)?,
    contenido: @Composable () -> Unit,
) {
    val gesto = rememberGestoVolver(onVolver)
    val progreso = gesto.progreso

    Box(Modifier.fillMaxSize()) {
        if (progreso > 0f && fondo != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = -size.width * 0.3f * (1f - gesto.progreso) },
            ) {
                fondo()
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.10f * (1f - progreso))),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = size.width * gesto.progreso }
                .sombraIzquierda(progreso),
        ) {
            contenido()
        }
    }
}
