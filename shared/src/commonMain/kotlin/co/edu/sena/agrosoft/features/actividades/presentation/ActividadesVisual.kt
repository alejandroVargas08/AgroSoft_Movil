package co.edu.sena.agrosoft.features.actividades.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.features.actividades.data.AsignacionActividad
import com.composables.icons.lucide.*

internal data class Insignia(val texto: String, val color: Color, val fondo: Color)

internal fun insigniaDe(s: Situacion): Insignia = when (s) {
    Situacion.POR_REVISAR -> Insignia("Por revisar", Color.White, AgroColors.Info)
    Situacion.VENCIDA -> Insignia("Vencida", Color.White, AgroColors.Error)
    Situacion.EN_CURSO -> Insignia("En curso", Color(0xFFE65100), AgroColors.AccentSoft)
    Situacion.COMPLETADA -> Insignia("Completada", AgroColors.PrimaryDark, AgroColors.SuccessSoft)
    Situacion.SIN_ASIGNAR -> Insignia("Sin asignar", AgroColors.TextSecondary, Color(0xFFF1F5F9))
    Situacion.CANCELADA -> Insignia("Cancelada", AgroColors.TextSecondary, Color(0xFFF1F5F9))
}

internal fun insigniaDe(a: AsignacionActividad): Insignia = when {
    a.estado == "APROBADA" -> Insignia("Aprobada", AgroColors.PrimaryDark, AgroColors.SuccessSoft)
    a.estado == "ENTREGADA" -> Insignia("Por revisar", Color.White, Color(0xFF8B5CF6))
    a.atrasada -> Insignia("Vencida", Color.White, AgroColors.Error)
    a.estado == "DEVUELTA" -> Insignia("Devuelta", AgroColors.Error, AgroColors.ErrorSoft)
    a.estado == "EN_PROGRESO" -> Insignia("En progreso", Color(0xFF1D4ED8), Color(0xFFDBEAFE))
    else -> Insignia("Pendiente", Color(0xFFE65100), AgroColors.AccentSoft)
}

internal fun iconoTipo(tipo: String): ImageVector {
    val t = tipo.lowercase()
    return when {
        "riego" in t -> Lucide.Droplets
        "fertiliz" in t || "siembra" in t -> Lucide.Sprout
        "cosecha" in t -> Lucide.Wheat
        "arvense" in t || "maleza" in t || "poda" in t -> Lucide.Scissors
        "monitoreo" in t || "fitosanit" in t -> Lucide.Search
        else -> Lucide.ClipboardCheck
    }
}

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

internal fun fechaTexto(texto: String?): String? {
    val partes = texto?.take(10)?.split("-") ?: return null
    if (partes.size != 3) return null
    val mes = partes[1].toIntOrNull()?.minus(1) ?: return null
    val dia = partes[2].toIntOrNull() ?: return null
    return "$dia ${MESES.getOrElse(mes) { "" }} ${partes[0]}"
}

@Composable
internal fun Pildora(i: Insignia) {
    Text(
        i.texto,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = i.color,
        modifier = Modifier.clip(CircleShape).background(i.fondo).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun Chip(texto: String, activo: Boolean, onClick: () -> Unit) {
    Text(
        texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (activo) Color.White else AgroColors.TextSecondary,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (activo) AgroColors.Primary else Color.White)
            .border(1.dp, if (activo) AgroColors.Primary else AgroColors.Border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
internal fun Franja(texto: String, exito: Boolean) {
    Text(
        texto,
        fontSize = 12.sp,
        color = if (exito) AgroColors.Primary else AgroColors.Error,
        lineHeight = 16.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (exito) AgroColors.SuccessSoft else AgroColors.ErrorSoft)
            .padding(12.dp),
    )
}
