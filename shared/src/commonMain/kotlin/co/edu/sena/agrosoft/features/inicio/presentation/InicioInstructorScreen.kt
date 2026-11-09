package co.edu.sena.agrosoft.features.inicio.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.core.util.decimal
import co.edu.sena.agrosoft.features.inicio.data.AprendizResumen
import com.composables.icons.lucide.*

@Composable
fun InicioInstructorScreen(
    sesionId: Int,
    onAbrirRevisar: () -> Unit,
    onAbrirSensores: () -> Unit,
) {
    val vm = viewModel(key = "inicio-instructor-$sesionId") { InicioInstructorViewModel() }
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        when {
            e.cargando && e.aprendices.isEmpty() -> Cargando()
            e.error != null && e.aprendices.isEmpty() -> ErrorCarga(e.error, vm::cargar)
            else -> {
                SelectorFicha(e, vm)

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Indicador(
                        valor = e.porRevisar.toString(),
                        etiqueta = "Entregas por revisar",
                        icono = Lucide.ClipboardCheck,
                        destacado = true,
                        onClick = onAbrirRevisar,
                        modifier = Modifier.weight(1f),
                    )
                    Indicador(
                        valor = e.novedades?.toString() ?: "—",
                        etiqueta = "Novedades pendientes",
                        icono = Lucide.MessageSquareWarning,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Indicador(
                        valor = e.vencidas.toString(),
                        etiqueta = "Tareas vencidas",
                        icono = Lucide.Clock,
                        alerta = e.vencidas > 0,
                        modifier = Modifier.weight(1f),
                    )
                    Indicador(
                        valor = e.sensoresEnAlerta?.toString() ?: "—",
                        etiqueta = "Estaciones con alerta",
                        icono = Lucide.Radio,
                        alerta = (e.sensoresEnAlerta ?: 0) > 0,
                        onClick = onAbrirSensores,
                        modifier = Modifier.weight(1f),
                    )
                }

                val lista = if (e.verTodos) e.visibles else e.visibles.take(5)
                TituloSeccion(
                    "Mis aprendices",
                    accion = if (e.visibles.size > 5) {
                        {
                            Text(
                                if (e.verTodos) "Ver menos" else "Ver todos (${e.visibles.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AgroColors.Primary,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.alternarTodos() }.padding(4.dp),
                            )
                        }
                    } else null,
                )

                if (lista.isEmpty()) {
                    Vacio()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        lista.forEach { ItemAprendiz(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorFicha(e: InicioInstructorEstado, vm: InicioInstructorViewModel) {
    var abierto by remember { mutableStateOf(false) }
    val elegible = e.fichas.size > 1

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .border(1.dp, AgroColors.Border, CircleShape)
                    .clickable(enabled = elegible) { abierto = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Lucide.Users, contentDescription = null, tint = AgroColors.PrimaryDark, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(e.etiquetaFicha, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AgroColors.PrimaryDark)
                if (elegible) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Lucide.ChevronDown, contentDescription = null, tint = AgroColors.PrimaryDark, modifier = Modifier.size(14.dp))
                }
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                DropdownMenuItem(
                    text = { Text("Todas mis fichas", fontSize = 13.sp) },
                    onClick = { vm.elegirFicha(null); abierto = false },
                )
                e.fichas.forEach { f ->
                    DropdownMenuItem(
                        text = { Text("Ficha $f", fontSize = 13.sp) },
                        onClick = { vm.elegirFicha(f); abierto = false },
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (e.visibles.size == 1) "1 aprendiz" else "${e.visibles.size} aprendices",
            fontSize = 12.sp,
            color = AgroColors.TextMuted,
        )
    }
}

@Composable
private fun Indicador(
    valor: String,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
    alerta: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val forma = RoundedCornerShape(20.dp)
    val fondo = if (destacado) AgroColors.Primary else AgroColors.Surface
    val textoValor = when {
        destacado -> Color.White
        alerta -> AgroColors.Error
        else -> AgroColors.Title
    }
    Column(
        modifier = modifier
            .clip(forma)
            .background(fondo)
            .border(1.dp, if (destacado) AgroColors.Primary else AgroColors.BorderCard, forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = when {
                destacado -> Color.White.copy(alpha = 0.85f)
                alerta -> AgroColors.Error
                else -> AgroColors.Primary
            },
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textoValor)
        Spacer(Modifier.height(2.dp))
        Text(
            etiqueta,
            fontSize = 11.sp,
            color = if (destacado) Color.White.copy(alpha = 0.75f) else AgroColors.TextMuted,
            maxLines = 2,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun ItemAprendiz(a: AprendizResumen) {
    val avance = if (a.tareas > 0) a.aprobadas.toFloat() / a.tareas else 0f
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(AgroColors.PrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(iniciales(a), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    a.nombreCompleto,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroColors.Title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        a.idFicha?.let { "Ficha $it" },
                        Fechas.haceCuanto(a.ultimoIngreso)?.let { "Ingresó: ${it.lowercase()}" },
                    ).joinToString(" · ").ifEmpty { "Sin ingresos" },
                    fontSize = 10.sp,
                    color = AgroColors.TextMuted,
                )

                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(AgroColors.SurfaceSoft),
                ) {
                    Box(Modifier.fillMaxWidth(avance).fillMaxHeight().clip(CircleShape).background(AgroColors.Primary))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${a.aprobadas} de ${a.tareas} aprobadas · ${decimal(a.horasTotales)} h",
                    fontSize = 11.sp,
                    color = AgroColors.TextSecondary,
                )

                if (a.entregadas > 0 || a.vencidas > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (a.entregadas > 0) {
                            Etiqueta(
                                if (a.entregadas == 1) "1 por revisar" else "${a.entregadas} por revisar",
                                AgroColors.AccentSoft,
                                AgroColors.Accent,
                            )
                        }
                        if (a.vencidas > 0) {
                            Etiqueta(
                                if (a.vencidas == 1) "1 vencida" else "${a.vencidas} vencidas",
                                AgroColors.ErrorSoft,
                                AgroColors.Error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Text(
        texto,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.clip(CircleShape).background(fondo).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun iniciales(a: AprendizResumen): String =
    listOf(a.nombre, a.apellido)
        .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

@Composable
private fun Vacio() {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.Users, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text("Aún no tienes aprendices a cargo.", fontSize = 13.sp, color = AgroColors.TextMuted, textAlign = TextAlign.Center)
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
            Text(
                "Reintentar",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AgroColors.Primary)
                    .clickable(onClick = onReintentar)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}
