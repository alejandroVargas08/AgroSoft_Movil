package co.edu.sena.agrosoft.features.revision.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.core.util.decimal
import co.edu.sena.agrosoft.features.revision.data.Entrega
import coil3.compose.AsyncImage
import com.composables.icons.lucide.*

@Composable
fun RevisionScreen(sesionId: Int) {
    val vm = viewModel(key = "revision-$sesionId") { RevisionViewModel() }
    val e = vm.estado
    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (!e.cargando || e.entregas.isNotEmpty()) {
            Text(
                when (e.entregas.size) {
                    0 -> "No hay entregas pendientes"
                    1 -> "1 entrega esperando tu revisión"
                    else -> "${e.entregas.size} entregas esperando tu revisión"
                },
                fontSize = 12.sp,
                color = AgroColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))
        }

        e.aviso?.let {
            Mensaje(it, AgroColors.PrimaryDark, AgroColors.SuccessSoft, Lucide.CircleCheck)
            Spacer(Modifier.height(12.dp))
        }

        when {
            e.cargando && e.entregas.isEmpty() -> Cargando()
            e.error != null && e.entregas.isEmpty() -> ErrorCarga(e.error, vm::cargar)
            e.entregas.isEmpty() -> Vacio()
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                e.entregas.forEach { TarjetaEntrega(it, e, vm) }
            }
        }
    }
}

@Composable
private fun TarjetaEntrega(entrega: Entrega, e: RevisionEstado, vm: RevisionViewModel) {
    val abierta = e.abierta == entrega.id
    val nombre = listOfNotNull(entrega.aprendiz?.nombre, entrega.aprendiz?.apellido).joinToString(" ").ifBlank { "Aprendiz" }
    val detalle = listOfNotNull(
        Fechas.haceCuanto(entrega.entregadaEn)?.let { "Entregada ${it.lowercase()}" },
        entrega.horasTrabajadas?.let { "${decimal(it)} h" },
    ).joinToString(" · ")

    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { vm.alternar(entrega.id) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AgroColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(iniciales(nombre), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nombre, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    entrega.actividad?.nombre ?: "Actividad",
                    fontSize = 12.sp,
                    color = AgroColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detalle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(detalle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (entrega.vencida) AgroColors.Accent else Color(0xFF6A7D6D))
                }
            }
            Icon(
                if (abierta) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = null,
                tint = Color(0xFF718176),
                modifier = Modifier.size(18.dp),
            )
        }

        if (abierta) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E9DF)))
                Spacer(Modifier.height(12.dp))

                entrega.actividad?.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 12.sp, color = AgroColors.TextMuted, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                }

                entrega.comentarioAprendiz?.takeIf { it.isNotBlank() }?.let {
                    Mensaje("Aprendiz: $it", AgroColors.Title, AgroColors.SurfaceSoft, Lucide.MessageCircle)
                    Spacer(Modifier.height(12.dp))
                }

                val fotos = entrega.actividad?.id?.let { e.fotos[it] }.orEmpty()
                Text(
                    if (fotos.isEmpty()) "Sin fotos de evidencia" else "Fotos de evidencia (${fotos.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroColors.Title,
                )
                if (fotos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        fotos.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Foto de evidencia",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(112.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AgroColors.SurfaceSoft),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                when (e.decision) {
                    null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Boton("Devolver", Estilo.PELIGRO, Modifier.weight(1f)) { vm.elegir(Decision.DEVOLVER) }
                        Boton("Aprobar", Estilo.PRIMARIO, Modifier.weight(1f)) { vm.elegir(Decision.APROBAR) }
                    }
                    else -> Formulario(entrega, nombre, e, vm)
                }
            }
        }
    }
}

@Composable
private fun Formulario(entrega: Entrega, nombre: String, e: RevisionEstado, vm: RevisionViewModel) {
    val devolver = e.decision == Decision.DEVOLVER
    Column {
        Text(
            if (devolver) "¿Qué debe corregir $nombre?" else "Comentario para $nombre (opcional)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AgroColors.Text,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = e.comentario,
            onValueChange = vm::cambiarComentario,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 14.sp, color = AgroColors.Text),
            placeholder = {
                Text(
                    if (devolver) "Falta la foto del área fertilizada…" else "Buen trabajo…",
                    fontSize = 14.sp,
                    color = AgroColors.Placeholder,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (devolver) AgroColors.Error else AgroColors.Primary,
                unfocusedBorderColor = AgroColors.Border,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = AgroColors.Primary,
            ),
        )
        e.errorAccion?.let {
            Spacer(Modifier.height(10.dp))
            Mensaje(it, AgroColors.Error, AgroColors.ErrorSoft, Lucide.CircleAlert)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Boton("Cancelar", Estilo.SECUNDARIO, Modifier.weight(1f), habilitado = !e.procesando) { vm.cancelar() }
            Boton(
                if (devolver) "Devolver tarea" else "Aprobar tarea",
                if (devolver) Estilo.PELIGRO_LLENO else Estilo.PRIMARIO,
                Modifier.weight(1f),
                cargando = e.procesando,
            ) { vm.confirmar(entrega) }
        }
    }
}

private enum class Estilo { PRIMARIO, SECUNDARIO, PELIGRO, PELIGRO_LLENO }

@Composable
private fun Boton(
    texto: String,
    estilo: Estilo,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    onClick: () -> Unit,
) {
    val activo = habilitado && !cargando
    val (fondo, textoColor, borde) = when (estilo) {
        Estilo.PRIMARIO -> Triple(AgroColors.Primary, Color.White, AgroColors.Primary)
        Estilo.SECUNDARIO -> Triple(Color.White, AgroColors.Primary, Color(0xFFCFE2D0))
        Estilo.PELIGRO -> Triple(Color.White, AgroColors.Error, AgroColors.Error)
        Estilo.PELIGRO_LLENO -> Triple(AgroColors.Error, Color.White, AgroColors.Error)
    }
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo.copy(alpha = if (activo || fondo == Color.White) 1f else 0.6f))
            .border(1.dp, borde, RoundedCornerShape(12.dp))
            .clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = textoColor)
        } else {
            Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textoColor)
        }
    }
}

@Composable
private fun Mensaje(texto: String, color: Color, fondo: Color, icono: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fondo).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Vacio() {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.ClipboardCheck, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text("No tienes entregas por revisar. ¡Todo al día!", fontSize = 13.sp, color = AgroColors.TextMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Cargando() {
    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun ErrorCarga(mensaje: String, onReintentar: () -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.WifiOff, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(mensaje, fontSize = 13.sp, color = AgroColors.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Boton("Reintentar", Estilo.PRIMARIO, Modifier.width(140.dp), onClick = onReintentar)
        }
    }
}

private fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
