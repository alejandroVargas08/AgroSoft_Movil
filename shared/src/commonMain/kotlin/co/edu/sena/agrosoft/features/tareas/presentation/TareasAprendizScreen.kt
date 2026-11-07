package co.edu.sena.agrosoft.features.tareas.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.core.util.decimal
import co.edu.sena.agrosoft.features.tareas.data.Tarea
import com.composables.icons.lucide.*
import androidx.compose.runtime.LaunchedEffect

@Composable
fun TareasAprendizScreen(sesionId: Int) {
    val vm = viewModel(key = "tareas-aprendiz-$sesionId") { TareasViewModel() }
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FiltroTareas.entries.forEach { f ->
                Chip("${f.etiqueta} ${e.de(f).size}", activo = f == e.filtro) { vm.cambiarFiltro(f) }
            }
        }

        e.aviso?.let {
            Spacer(Modifier.height(12.dp))
            Mensaje(it, AgroColors.PrimaryDark, AgroColors.SuccessSoft, Lucide.CircleCheck)
        }

        Spacer(Modifier.height(16.dp))
        when {
            e.cargando && e.tareas.isEmpty() -> Cargando()
            e.error != null && e.tareas.isEmpty() -> ErrorCarga(e.error, vm::cargar)
            else -> {
                val lista = e.de(e.filtro)
                if (lista.isEmpty()) {
                    Vacio(e.filtro)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        lista.forEach { tarea -> TarjetaTarea(tarea, e, vm) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaTarea(t: Tarea, e: TareasEstado, vm: TareasViewModel) {
    val abierta = e.abierta == t.id
    val entregando = e.entregando == t.id
    val (estadoTexto, urgente) = lineaEstado(t)
    val borde = if (t.estado == "DEVUELTA") Color(0xFFF5C2C2) else AgroColors.Border

    Tarjeta(modifier = Modifier.fillMaxWidth(), borde = borde) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { vm.alternar(t.id) }.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(AgroColors.SuccessSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Leaf, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.actividad?.nombre ?: "Tarea asignada",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroColors.Title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitulo(t)?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 11.sp, color = Color(0xFF5F7565))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    estadoTexto,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (urgente) AgroColors.Accent else Color(0xFF6A7D6D),
                )
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

                t.actividad?.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 12.sp, color = AgroColors.Text, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                }

                t.asignadoPor?.let {
                    Dato("Asignada por", listOfNotNull(it.nombre, it.apellido).joinToString(" "))
                }
                t.horasTrabajadas?.let { Dato("Horas reportadas", "${decimal(it)} h") }
                t.comentarioAprendiz?.takeIf { it.isNotBlank() }?.let { Dato("Tu comentario", it) }

                if (t.estado == "DEVUELTA" && !t.comentarioInstructor.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Mensaje("Instructor: ${t.comentarioInstructor}", AgroColors.Error, AgroColors.ErrorSoft, Lucide.RotateCcw)
                    Spacer(Modifier.height(12.dp))
                }
                if (t.estado == "APROBADA" && !t.comentarioInstructor.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Mensaje("Instructor: ${t.comentarioInstructor}", AgroColors.PrimaryDark, AgroColors.SuccessSoft, Lucide.MessageCircle)
                    Spacer(Modifier.height(12.dp))
                }

                when {
                    entregando -> FormularioEntrega(t, e, vm)
                    t.estado == "ASIGNADA" -> Boton("Iniciar tarea", primario = true, cargando = e.procesando) { vm.iniciar(t.id) }
                    t.estado == "EN_PROGRESO" -> Boton("Entregar tarea", primario = true) { vm.empezarEntrega(t.id) }
                    t.estado == "DEVUELTA" -> Boton("Corregir y entregar", primario = true) { vm.empezarEntrega(t.id) }
                    t.estado == "ENTREGADA" -> Text(
                        "Tu instructor está revisando esta entrega.",
                        fontSize = 11.sp,
                        color = AgroColors.TextMuted,
                    )
                }

                if (!entregando) {
                    e.errorAccion?.let {
                        Spacer(Modifier.height(10.dp))
                        Mensaje(it, AgroColors.Error, AgroColors.ErrorSoft, Lucide.CircleAlert)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormularioEntrega(t: Tarea, e: TareasEstado, vm: TareasViewModel) {
    Column {
        Text("Horas trabajadas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
        Spacer(Modifier.height(6.dp))
        Campo(
            valor = e.horas,
            onCambio = vm::cambiarHoras,
            marcador = "Ej: 2,5",
            teclado = KeyboardType.Decimal,
            unaLinea = true,
        )
        Spacer(Modifier.height(12.dp))
        Text("Comentario (opcional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
        Spacer(Modifier.height(6.dp))
        Campo(
            valor = e.comentario,
            onCambio = vm::cambiarComentario,
            marcador = "¿Qué hiciste? ¿Hubo alguna novedad?",
            teclado = KeyboardType.Text,
            unaLinea = false,
        )
        e.errorAccion?.let {
            Spacer(Modifier.height(10.dp))
            Mensaje(it, AgroColors.Error, AgroColors.ErrorSoft, Lucide.CircleAlert)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Boton("Cancelar", primario = false, modifier = Modifier.weight(1f), habilitado = !e.procesando) { vm.cancelarEntrega() }
            Boton("Enviar entrega", primario = true, modifier = Modifier.weight(1f), cargando = e.procesando) { vm.entregar(t.id) }
        }
    }
}

@Composable
private fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    marcador: String,
    teclado: KeyboardType,
    unaLinea: Boolean,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = Modifier.fillMaxWidth().then(if (unaLinea) Modifier else Modifier.heightIn(min = 96.dp)),
        singleLine = unaLinea,
        shape = RoundedCornerShape(12.dp),
        textStyle = TextStyle(fontSize = 14.sp, color = AgroColors.Text),
        placeholder = { Text(marcador, fontSize = 14.sp, color = AgroColors.Placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroColors.Primary,
            unfocusedBorderColor = AgroColors.Border,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = AgroColors.Primary,
        ),
    )
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(etiqueta, fontSize = 11.sp, color = AgroColors.TextMuted, modifier = Modifier.width(120.dp))
        Text(valor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Title)
    }
}

@Composable
private fun Chip(texto: String, activo: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (activo) AgroColors.Primary else Color.White.copy(alpha = 0.65f))
            .border(1.dp, if (activo) AgroColors.Primary else Color.White, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (activo) Color.White else Color(0xFF607366),
        )
    }
}

@Composable
private fun Boton(
    texto: String,
    primario: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    habilitado: Boolean = true,
    cargando: Boolean = false,
    onClick: () -> Unit,
) {
    val activo = habilitado && !cargando
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primario) AgroColors.Primary.copy(alpha = if (activo) 1f else 0.6f) else Color.White)
            .border(1.dp, if (primario) Color.Transparent else Color(0xFFCFE2D0), RoundedCornerShape(12.dp))
            .clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Text(
                texto,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (primario) Color.White else AgroColors.Primary,
            )
        }
    }
}

@Composable
private fun Mensaje(texto: String, color: Color, fondo: Color, icono: androidx.compose.ui.graphics.vector.ImageVector) {
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
private fun Vacio(filtro: FiltroTareas) {
    val texto = when (filtro) {
        FiltroTareas.PENDIENTES -> "No tienes tareas pendientes. ¡Estás al día!"
        FiltroTareas.ENTREGADAS -> "No tienes entregas esperando revisión."
        FiltroTareas.APROBADAS -> "Todavía no tienes tareas aprobadas."
    }
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.ClipboardCheck, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(texto, fontSize = 13.sp, color = AgroColors.TextMuted, textAlign = TextAlign.Center)
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
            Boton("Reintentar", primario = true, modifier = Modifier.width(140.dp), onClick = onReintentar)
        }
    }
}

private fun subtitulo(t: Tarea): String? =
    listOfNotNull(
        t.actividad?.tipo?.replaceFirstChar { it.uppercase() },
        t.actividad?.subtipo?.replaceFirstChar { it.uppercase() },
    ).joinToString(" · ").ifBlank { null }

private fun lineaEstado(t: Tarea): Pair<String, Boolean> = when (t.estado) {
    "DEVUELTA" -> "Devuelta · corrige y vuelve a entregar" to true
    "ENTREGADA" -> "Entregada · en revisión" to false
    "APROBADA" -> "Aprobada" to false
    else -> {
        val etiqueta = Fechas.etiquetaLimite(t.fechaLimite)
        val horas = Fechas.horasHasta(t.fechaLimite)
        val prefijo = if (t.estado == "EN_PROGRESO") "En progreso · " else ""
        when {
            t.vencida || etiqueta == "Vencida" -> "${prefijo}Vencida" to true
            etiqueta != null -> "${prefijo}Vence ${etiqueta.replaceFirstChar { it.lowercase() }}" to (horas != null && horas <= 24)
            t.estado == "EN_PROGRESO" -> "En progreso" to false
            else -> "Sin fecha límite" to false
        }
    }
}
