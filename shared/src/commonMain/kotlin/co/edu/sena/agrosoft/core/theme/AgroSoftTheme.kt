package co.edu.sena.agrosoft.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AgroColors {
    // Verdes
    val Primary = Color(0xFF2D7A3E)       // botones, activo
    val PrimaryDark = Color(0xFF1A4D2E)   // íconos, texto verde oscuro
    val Title = Color(0xFF173F29)         // títulos de tarjetas
    val PrimarySoft = Color(0xFFEBF3ED)   // fondo de íconos
    val Chip = Color(0xFF569058)          // etiquetas pequeñas
    val SuccessSoft = Color(0xFFE8F5E9)

    // Fondos
    val Background = Color(0xFFF9FAF7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFF5F8F4)   // casillas de clima

    // Textos
    val Text = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val TextMuted = Color(0xFF627668)
    val Placeholder = Color(0xFF9CA3AF)

    // Bordes
    val Border = Color(0xFFE5E7EB)
    val BorderCard = Color(0xFFE5E7EB)

    // Estados
    val Accent = Color(0xFFF16C07)        //avisos, badges
    val AccentSoft = Color(0xFFFFF3E0)
    val Error = Color(0xFFDC2626)
    val ErrorSoft = Color(0xFFFEF2F2)
    val Warning = Color(0xFFB45309)
    val WarningSoft = Color(0xFFFFFBEB)
    val Info = Color(0xFF3B82F6)
    val Success = Color(0xFF10B981)
}

private val esquemaClaro = lightColorScheme(
    primary = AgroColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AgroColors.PrimarySoft,
    onPrimaryContainer = AgroColors.PrimaryDark,
    secondary = AgroColors.Accent,
    onSecondary = Color.White,
    background = AgroColors.Background,
    onBackground = AgroColors.Text,
    surface = AgroColors.Surface,
    onSurface = AgroColors.Text,
    onSurfaceVariant = AgroColors.TextSecondary,
    outline = AgroColors.Border,
    error = AgroColors.Error,
    onError = Color.White,
)

@Composable
fun AgroSoftTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquemaClaro, content = content)
}
