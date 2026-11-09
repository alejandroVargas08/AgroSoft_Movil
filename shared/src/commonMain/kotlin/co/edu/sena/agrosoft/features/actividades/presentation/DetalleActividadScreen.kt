package co.edu.sena.agrosoft.features.actividades.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.actividades.data.AsignacionActividad
import co.edu.sena.agrosoft.features.actividades.data.comoNumero
import com.composables.icons.lucide.*

@Composable
fun DetalleActividadScreen(vm: ActividadesViewModel, onRevisar: () -> Unit) {
    val e = vm.estado
    val r = e.seleccionada

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (r == null) {
            Vacio(Lucide.ClipboardCheck, "Selecciona una actividad de la lista.")
            return@Column
        }
        val a = r.actividad

        Tarjeta(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            a.tipo.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = AgroColors.Chip,
                        )
                        Text(a.titulo, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
                        Text(e.nombreLote(a.loteId), fontSize = 12.sp, color = AgroColors.TextSecondary)
                    }
                    Pildora(insigniaDe(r.situacion))
                }
                a.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = AgroColors.TextSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AgroColors.Background).padding(12.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    Dato("Fecha límite", fechaTexto(r.fechaLimite) ?: "Sin límite", Modifier.weight(1f))
                    Dato("Entregas", "${r.entregadas} de ${r.total}", Modifier.weight(1f), AgroColors.Primary)
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    Dato("Horas estimadas", a.horasActividad.comoNumero()?.let { "${it.toInt()} h" } ?: "—", Modifier.weight(1f))
                    Dato("Por revisar", r.porRevisar.toString(), Modifier.weight(1f), if (r.porRevisar > 0) AgroColors.Info else AgroColors.Text)
                }
            }
        }

        e.avisoDetalle?.let {
            Spacer(Modifier.height(12.dp))
            Franja(it, exito = true)
        }
        e.errorDetalle?.let {
            Spacer(Modifier.height(12.dp))
            Franja(it, exito = false)
        }

        TituloSeccion("Aprendices")
        when {
            e.cargandoDetalle && r.asignaciones.isEmpty() -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
            }
            r.asignaciones.isEmpty() -> Vacio(Lucide.Users, "Esta actividad todavía no tiene aprendices asignados.")
            else -> Tarjeta(modifier = Modifier.fillMaxWidth()) {
                r.asignaciones
                    .sortedWith(compareBy<AsignacionActividad> { orden(it) }.thenBy { it.aprendiz?.completo })
                    .forEachIndexed { i, asg ->
                        if (i > 0) HorizontalDivider(color = AgroColors.Border, modifier = Modifier.padding(horizontal = 16.dp))
                        FilaAprendiz(asg, e.procesando, onRevisar) { vm.quitar(asg.id) }
                    }
            }
        }

        if (a.activa) {
            Spacer(Modifier.height(14.dp))
            if (e.panelAsignar) {
                PanelAsignar(e, vm)
            } else {
                OutlinedButton(
                    onClick = vm::abrirPanelAsignar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AgroColors.Primary),
                ) {
                    Icon(Lucide.UserPlus, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Asignar más aprendices", color = AgroColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = vm::pedirCancelar,
                enabled = !e.procesando,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AgroColors.Error),
            ) {
                Text("Cancelar actividad", color = AgroColors.Error, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (e.confirmarCancelar) {
        AlertDialog(
            onDismissRequest = vm::descartarCancelar,
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("¿Cancelar actividad?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text) },
            text = {
                Text(
                    "Los aprendices dejarán de verla como una tarea activa. Esta acción no se puede deshacer.",
                    fontSize = 14.sp,
                    color = AgroColors.TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = vm::cancelarActividad,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Error),
                ) { Text("Cancelar actividad", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(onClick = vm::descartarCancelar, shape = RoundedCornerShape(12.dp)) {
                    Text("Volver", color = AgroColors.Text, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

private fun orden(a: AsignacionActividad): Int = when {
    a.estado == "ENTREGADA" -> 0
    a.atrasada -> 1
    a.estado == "DEVUELTA" -> 2
    a.estado == "APROBADA" -> 4
    else -> 3
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier, color: Color = AgroColors.Text) {
    Column(modifier) {
        Text(etiqueta, fontSize = 10.sp, color = AgroColors.TextSecondary)
        Text(valor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun FilaAprendiz(a: AsignacionActividad, procesando: Boolean, onRevisar: () -> Unit, onQuitar: () -> Unit) {
    val nombre = a.aprendiz?.completo ?: "Aprendiz"
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8F0EA)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AgroColors.PrimaryDark,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pildora(insigniaDe(a))
                val detalle = listOfNotNull(
                    a.horasTrabajadas.comoNumero()?.let { "${it} h" },
                    Fechas.haceCuanto(a.entregadaEn)?.lowercase(),
                ).joinToString(" · ")
                if (detalle.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(detalle, fontSize = 10.sp, color = AgroColors.TextMuted)
                }
            }
            a.comentarioAprendiz?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text("\"$it\"", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = AgroColors.TextSecondary, maxLines = 2)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            a.estado == "ENTREGADA" -> Text(
                "Revisar",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AgroColors.Primary).clickable(onClick = onRevisar)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            a.estado != "APROBADA" -> Box(
                Modifier.size(32.dp).clip(CircleShape).clickable(enabled = !procesando, onClick = onQuitar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.X, contentDescription = "Quitar", tint = AgroColors.TextMuted, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun PanelAsignar(e: ActividadesEstado, vm: ActividadesViewModel) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Asignar a", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text, modifier = Modifier.weight(1f))
                Icon(Lucide.X, contentDescription = "Cerrar", tint = AgroColors.TextMuted, modifier = Modifier.size(18.dp).clickable { vm.cerrarPanelAsignar() })
            }
            Spacer(Modifier.height(8.dp))
            if (e.disponiblesParaAsignar.isEmpty()) {
                Text("Todos tus aprendices ya tienen esta actividad.", fontSize = 12.sp, color = AgroColors.TextMuted)
            } else {
                e.disponiblesParaAsignar.forEach { ap ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { vm.alternarAsignar(ap.id) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = ap.id in e.seleccionAsignar,
                            onCheckedChange = { vm.alternarAsignar(ap.id) },
                            colors = CheckboxDefaults.colors(checkedColor = AgroColors.Primary),
                        )
                        Text(ap.nombreCompleto, fontSize = 13.sp, color = AgroColors.Text, modifier = Modifier.weight(1f))
                        ap.idFicha?.let { Text(it, fontSize = 10.sp, color = AgroColors.TextMuted) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Plazo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Plazo.entries.forEach { p -> Chip(p.etiqueta, p == e.plazoAsignar) { vm.cambiarPlazoAsignar(p) } }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = vm::confirmarAsignar,
                    enabled = e.seleccionAsignar.isNotEmpty() && !e.procesando,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
                ) {
                    if (e.procesando) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            if (e.seleccionAsignar.isEmpty()) "Elige aprendices" else "Asignar a ${e.seleccionAsignar.size}",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
