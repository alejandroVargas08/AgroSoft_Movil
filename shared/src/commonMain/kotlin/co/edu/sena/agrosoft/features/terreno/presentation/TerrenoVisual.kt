package co.edu.sena.agrosoft.features.terreno.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import co.edu.sena.agrosoft.core.network.ApiCliente
import co.edu.sena.agrosoft.core.network.ApiConfig
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.features.terreno.data.Vuelo
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.composables.icons.lucide.ImageOff
import com.composables.icons.lucide.Lucide
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal fun rutaImagen(vueloId: Int, capturaId: Int, capa: Capa, completa: Boolean = true): String =
    if (capa == Capa.RGB) {
        "vuelos/$vueloId/capturas/$capturaId/imagen?banda=RGB&tamano=${if (completa) "completa" else "miniatura"}"
    } else {
        "vuelos/$vueloId/capturas/$capturaId/capa?indice=${capa.name}"
    }

@Composable
internal fun ImagenApi(
    ruta: String,
    descripcion: String?,
    modifier: Modifier = Modifier,
    escala: ContentScale = ContentScale.Crop,
) {
    val contexto = LocalPlatformContext.current
    val token = ApiCliente.token
    val solicitud = remember(ruta, token) {
        ImageRequest.Builder(contexto)
            .data(ApiConfig.BASE_URL + ruta)
            .apply {
                if (token != null) {
                    httpHeaders(NetworkHeaders.Builder().set("Authorization", "Bearer $token").build())
                }
            }
            .build()
    }
    SubcomposeAsyncImage(
        model = solicitud,
        contentDescription = descripcion,
        contentScale = escala,
        modifier = modifier,
        loading = {
            Box(Modifier.fillMaxSize().background(Color(0xFFE8F0EA)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
        },
        error = {
            Box(Modifier.fillMaxSize().background(Color(0xFFE8F0EA)), contentAlignment = Alignment.Center) {
                Icon(Lucide.ImageOff, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(24.dp))
            }
        },
    )
}

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

internal fun fechaVuelo(texto: String?): String {
    val partes = texto?.take(10)?.split("-") ?: return "Sin fecha"
    if (partes.size != 3) return "Sin fecha"
    val mes = partes[1].toIntOrNull()?.minus(1) ?: return "Sin fecha"
    val dia = partes[2].toIntOrNull() ?: return "Sin fecha"
    return "$dia ${MESES.getOrElse(mes) { "" }} ${partes[0]}"
}

internal fun porcentaje(valor: Double?): String = valor?.let { "${it.roundToInt()} %" } ?: "—"

internal fun indice(valor: Double): String {
    val r = (valor * 100).roundToLong()
    val a = abs(r)
    return "${if (r < 0) "-" else ""}${a / 100},${(a % 100).toString().padStart(2, '0')}"
}

internal fun hectareas(valor: Double?): String {
    if (valor == null) return "—"
    val r = (valor * 10).roundToLong()
    return "${r / 10},${r % 10} ha"
}

internal data class Etiqueta(val texto: String, val color: Color, val fondo: Color)

internal fun estadoDe(v: Vuelo): Etiqueta = when {
    v.enProceso -> Etiqueta("Analizando", Color(0xFF1D4ED8), Color(0xFFDBEAFE))
    v.analizado -> Etiqueta("Analizado", Color(0xFF047857), Color(0xFFD1FAE5))
    v.estado == "ERROR" || v.ultimoAnalisis?.estado == "ERROR" -> Etiqueta("Con error", AgroColors.Error, AgroColors.ErrorSoft)
    v.estado == "CON_IMAGENES" -> Etiqueta("Con imágenes", AgroColors.Warning, AgroColors.WarningSoft)
    else -> Etiqueta("Registrado", AgroColors.TextSecondary, Color(0xFFF1F5F9))
}

internal fun saludDe(pct: Double?): Etiqueta = when {
    pct == null -> Etiqueta("SIN DATOS", AgroColors.TextSecondary, Color(0xFFF1F5F9))
    pct >= 70 -> Etiqueta("SALUDABLE", Color(0xFF047857), Color(0xFFD1FAE5))
    pct >= 40 -> Etiqueta("ESTRÉS MODERADO", Color(0xFFB45309), Color(0xFFFEF3C7))
    else -> Etiqueta("ESTRÉS ALTO", AgroColors.Error, AgroColors.ErrorSoft)
}

internal val CLASES_NDVI = listOf(
    Triple("SALUDABLE", "Saludable", Color(0xFF10B981)),
    Triple("ESTRES_MODERADO", "Estrés moderado", Color(0xFFF59E0B)),
    Triple("ESTRES_ALTO", "Estrés alto", Color(0xFFDC2626)),
    Triple("SIN_VEGETACION", "Sin vegetación", Color(0xFF9CA3AF)),
)
