package co.edu.sena.agrosoft.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.navigation.Pantalla
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.theme.AgroColors
import com.composables.icons.lucide.*
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.glass.*

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

    BoxWithConstraints(
        modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 20.dp,
                shape = forma,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f),
            )
            .hazeGlass(
                input = HazeInput.Backdrop(hazeState),
                style = GlassStyle {
                    tint(Color.White.copy(alpha = 0.45f))
                    shape(forma)
                    surfaceProfile(SurfaceProfile.Squircle)
                    specularIntensity(0.7f)
                    ambientResponse(0.4f)
                    edgeSoftness(10.dp)
                },
            )
            .padding(6.dp),
    ) {
        val ancho = maxWidth / pestanas.size
        val posicion by animateDpAsState(
            targetValue = ancho * indice,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
            label = "burbuja",
        )

        Box(
            Modifier
                .offset(x = posicion)
                .width(ancho)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(AgroColors.Primary.copy(alpha = 0.14f)),
        )

        Row(Modifier.fillMaxSize()) {
            pestanas.forEach { p ->
                ItemCristal(
                    pestana = p,
                    activa = p.pantalla == actual,
                    insignia = insignias[p.pantalla] ?: 0,
                    modifier = Modifier.weight(1f),
                ) { onCambiar(p.pantalla) }
            }
        }
    }
}

@Composable
private fun ItemCristal(
    pestana: PestanaCristal,
    activa: Boolean,
    insignia: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        if (activa) AgroColors.Primary else Color(0xFF374151),
        label = "color",
    )
    Column(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
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
            fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
        )
    }
}
