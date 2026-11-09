package co.edu.sena.agrosoft.features.agrobot.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.core.util.Fechas
import com.composables.icons.lucide.*

private fun sugerenciasDe(rol: Rol): List<String> = when (rol) {
    Rol.APRENDIZ -> listOf(
        "¿Puedo trabajar con el UV de hoy?",
        "¿Cómo está la humedad del lote?",
        "¿Qué debo tener en cuenta en un riego?",
        "Explícame qué es el NDVI",
    )
    Rol.INSTRUCTOR -> listOf(
        "¿Qué lotes tienen estrés?",
        "¿Qué lotes tuvieron humedad alta?",
        "Resume el último vuelo de dron",
        "Sugiere tareas para esta semana",
    )
    Rol.ADMINISTRADOR -> listOf(
        "Resumen de alertas de hoy",
        "¿Qué insumos están bajos?",
        "Ingresos vs egresos del mes",
        "¿Cómo están las estaciones?",
    )
}

@Composable
fun AgroBotScreen(vm: AgroBotViewModel, nombre: String, rol: Rol, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { vm.cargar() }
    if (vm.estado.abierta) Chat(vm, modifier) else Entrada(vm, nombre, rol, modifier)
}

@Composable
private fun Entrada(vm: AgroBotViewModel, nombre: String, rol: Rol, modifier: Modifier) {
    val e = vm.estado
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 110.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(AgroColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Bot, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Hola, $nombre.", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Text("¿En qué te ayudo hoy?", fontSize = 14.sp, color = AgroColors.TextMuted)
        }

        TituloSeccion("Sugerencias para ti")
        sugerenciasDe(rol).chunked(2).forEach { fila ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEachIndexed { i, s ->
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 94.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, AgroColors.BorderCard, RoundedCornerShape(20.dp))
                            .clickable { vm.enviar(s) }
                            .padding(12.dp),
                    ) {
                        Icon(
                            if (i == 0) Lucide.MessageCircle else Lucide.Sparkles,
                            contentDescription = null,
                            tint = AgroColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(s, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF264B32), lineHeight = 17.sp)
                    }
                }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AgroColors.Primary)
                .clickable { vm.nueva() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nueva conversación", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        TituloSeccion("Conversaciones recientes")
        when {
            e.cargando -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            }
            e.error != null && e.conversaciones.isEmpty() -> Text(e.error, fontSize = 12.sp, color = AgroColors.Error)
            e.conversaciones.isEmpty() -> Text("Aún no tienes conversaciones.", fontSize = 12.sp, color = AgroColors.TextMuted)
            else -> Tarjeta(modifier = Modifier.fillMaxWidth()) {
                e.conversaciones.take(10).forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider(color = AgroColors.Border, modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.abrir(c) }.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Lucide.Bot, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                c.titulo?.takeIf { it.isNotBlank() } ?: "Conversación",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgroColors.Title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(Fechas.haceCuanto(c.actualizadoEn ?: c.creadoEn) ?: "", fontSize = 10.sp, color = AgroColors.TextMuted)
                        }
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).clickable { vm.eliminar(c) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Lucide.Trash2, contentDescription = "Eliminar", tint = AgroColors.TextMuted, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chat(vm: AgroBotViewModel, modifier: Modifier) {
    val e = vm.estado
    val lista = rememberLazyListState()
    val total = e.mensajes.size + 1 + if (e.enviando) 1 else 0

    LaunchedEffect(e.mensajes.size, e.enviando) {
        if (total > 0) lista.animateScrollToItem(total - 1)
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            state = lista,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Burbuja(
                    MensajeChat(false, "Hola, soy AgroBot. Puedo ayudarte con tus tareas, cultivos, estaciones y análisis de dron."),
                    pie = false,
                )
            }
            if (e.cargandoMensajes) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                }
            }
            itemsIndexed(e.mensajes) { i, m ->
                val ultimoDelBot = !m.deUsuario && i == e.mensajes.indexOfLast { !it.deUsuario }
                Burbuja(m, pie = ultimoDelBot && !m.error, onReintentar = if (m.error && i == e.mensajes.lastIndex) vm::reintentar else null)
            }
            if (e.enviando) {
                item { Escribiendo() }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(AgroColors.Background)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.dp, AgroColors.Border, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                if (e.texto.isEmpty()) {
                    Text("Escribe tu pregunta…", fontSize = 14.sp, color = AgroColors.Placeholder)
                }
                BasicTextField(
                    value = e.texto,
                    onValueChange = vm::cambiarTexto,
                    textStyle = TextStyle(fontSize = 14.sp, color = AgroColors.Text),
                    cursorBrush = SolidColor(AgroColors.Primary),
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            val activo = e.texto.isNotBlank() && !e.enviando
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (activo) AgroColors.Primary else AgroColors.Border)
                    .clickable(enabled = activo) { vm.enviar() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun Burbuja(m: MensajeChat, pie: Boolean, onReintentar: (() -> Unit)? = null) {
    if (m.deUsuario) {
        Row(Modifier.fillMaxWidth().padding(start = 48.dp), horizontalArrangement = Arrangement.End) {
            Text(
                m.texto,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(AgroColors.Primary)
                    .padding(14.dp),
            )
        }
        return
    }
    Row(Modifier.fillMaxWidth().padding(end = 32.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(if (m.error) AgroColors.Error else AgroColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (m.error) Lucide.TriangleAlert else Lucide.Bot, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(if (m.error) AgroColors.ErrorSoft else Color.White)
                .padding(14.dp),
        ) {
            Text(
                formatear(m.texto),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = if (m.error) AgroColors.Error else Color(0xFF304B37),
            )
            if (onReintentar != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Reintentar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroColors.Error,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onReintentar).padding(4.dp),
                )
            }
            if (pie) {
                Spacer(Modifier.height(10.dp))
                Text("AgroBot puede equivocarse. Confirma con tu instructor.", fontSize = 9.sp, color = Color(0xFF77867B))
            }
        }
    }
}

@Composable
private fun Escribiendo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(AgroColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.Bot, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("AgroBot está escribiendo…", fontSize = 12.sp, color = AgroColors.TextMuted)
        }
    }
}

private fun formatear(texto: String): AnnotatedString = buildAnnotatedString {
    texto.trim().lines().forEachIndexed { i, original ->
        if (i > 0) append("\n")
        val t = original.trimStart()
        when {
            t.startsWith("#") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { negritas(t.trimStart('#', ' ')) }
            t.startsWith("- ") || t.startsWith("* ") -> {
                append("•  ")
                negritas(t.drop(2))
            }
            else -> negritas(original)
        }
    }
}

private fun AnnotatedString.Builder.negritas(linea: String) {
    linea.split("**").forEachIndexed { i, parte ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parte) } else append(parte)
    }
}
