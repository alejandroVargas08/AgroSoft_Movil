package co.edu.sena.agrosoft.core.ui

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_agrosoft
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.navigation.Pantalla
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.theme.AgroColors
import com.composables.icons.lucide.*
import org.jetbrains.compose.resources.painterResource

// Sombra verde suave
private val SombraVerde = Color(0x1A1A4D2E)

//Tarjetas

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    fondo: Color = AgroColors.Surface,
    borde: Color = AgroColors.Border,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .shadow(8.dp, forma, ambientColor = SombraVerde, spotColor = SombraVerde)
            .clip(forma)
            .background(fondo)
            .border(1.dp, borde, forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = contenido,
    )
}

@Composable
fun TituloSeccion(texto: String, accion: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
        accion?.invoke()
    }
}

//Barra superior

@Composable
fun BarraSuperior(
    titulo: String,
    subtitulo: String?,
    esInicio: Boolean,
    iniciales: String,
    notificaciones: Int,
    onVolver: (() -> Unit)?,
    onAcciones: () -> Unit,
    onCampana: () -> Unit,
    onAvatar: () -> Unit,
) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, forma, ambientColor = SombraVerde, spotColor = SombraVerde)
            .clip(forma)
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.7f), forma)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La flecha de volver
        when {
            onVolver != null -> BotonIcono(
                icono = Lucide.ArrowLeft,
                descripcion = "Volver",
                fondo = AgroColors.PrimarySoft,
                onClick = onVolver,
            )
            esInicio -> Image(
                painter = painterResource(Res.drawable.logo_agrosoft),
                contentDescription = "AgroSoft",
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(start = 4.dp).size(40.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        Spacer(Modifier.width(10.dp))

        // Título
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AgroColors.Title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitulo != null) {
                Text(
                    subtitulo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AgroColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Campanita
        BotonIcono(Lucide.Plus, "Acciones rápidas", onClick = onAcciones)
        Box {
            BotonIcono(Lucide.Bell, "Notificaciones", onClick = onCampana)
            if (notificaciones > 0) {
                Insignia(
                    numero = notificaciones,
                    color = AgroColors.Error,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 2.dp, end = 4.dp)
                .size(40.dp)
                .border(2.dp, Color.White, CircleShape)
                .clip(CircleShape)
                .background(AgroColors.Primary)
                .clickable(onClick = onAvatar),
            contentAlignment = Alignment.Center,
        ) {
            Text(iniciales, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun BotonIcono(
    icono: ImageVector,
    descripcion: String,
    fondo: Color = Color.Transparent,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fondo)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = descripcion, tint = AgroColors.PrimaryDark, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun Insignia(numero: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$numero", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

//Barra inferior

private data class Pestana(val pantalla: Pantalla, val etiqueta: String, val icono: ImageVector)

private fun pestanasDe(rol: Rol): List<Pestana> = when (rol) {
    Rol.APRENDIZ -> listOf(
        Pestana(Pantalla.INICIO, "Inicio", Lucide.House),
        Pestana(Pantalla.TRABAJO, "Tareas", Lucide.ClipboardCheck),
        Pestana(Pantalla.AGROBOT, "AgroBot", Lucide.Bot),
        Pestana(Pantalla.TERRENO, "Terreno", Lucide.Map),
        Pestana(Pantalla.MAS, "Más", Lucide.Menu),
    )
    Rol.INSTRUCTOR -> listOf(
        Pestana(Pantalla.INICIO, "Inicio", Lucide.House),
        Pestana(Pantalla.TRABAJO, "Revisar", Lucide.ClipboardCheck),
        Pestana(Pantalla.AGROBOT, "AgroBot", Lucide.Bot),
        Pestana(Pantalla.TERRENO, "Terreno", Lucide.Map),
        Pestana(Pantalla.MAS, "Más", Lucide.Menu),
    )
    Rol.ADMINISTRADOR -> listOf(
        Pestana(Pantalla.INICIO, "Inicio", Lucide.House),
        Pestana(Pantalla.TRABAJO, "Solicitudes", Lucide.Users),
        Pestana(Pantalla.AGROBOT, "AgroBot", Lucide.Bot),
        Pestana(Pantalla.ALERTAS, "Alertas", Lucide.TriangleAlert),
        Pestana(Pantalla.MAS, "Más", Lucide.Menu),
    )
}


 //@param insignias número naranja sobre una pestaña

@Composable
fun BarraInferior(
    rol: Rol,
    actual: Pantalla,
    insignias: Map<Pantalla, Int>,
    onCambiar: (Pantalla) -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(32.dp)
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = 456.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(8.dp, forma, ambientColor = SombraVerde, spotColor = SombraVerde)
            .clip(forma)
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.7f), forma)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pestanasDe(rol).forEach { pestana ->
            val activa = pestana.pantalla == actual
            val color = if (activa) Color.White else AgroColors.TextMuted
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (activa) AgroColors.Primary.copy(alpha = 0.9f) else Color.Transparent)
                    .clickable { onCambiar(pestana.pantalla) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(pestana.icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        pestana.etiqueta,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        maxLines = 1,
                    )
                }
                val numero = insignias[pestana.pantalla]
                if (numero != null && numero > 0 && !activa) {
                    Insignia(
                        numero = numero,
                        color = AgroColors.Accent,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 10.dp),
                    )
                }
            }
        }
    }
}
