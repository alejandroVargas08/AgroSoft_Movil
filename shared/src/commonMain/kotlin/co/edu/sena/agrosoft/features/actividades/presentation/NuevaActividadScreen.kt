package co.edu.sena.agrosoft.features.actividades.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta

@Composable
fun NuevaActividadScreen(vm: ActividadesViewModel) {
    val e = vm.estado
    val f = e.form

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Seccion("¿Qué hay que hacer?") {
            Etiqueta("Tipo")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TIPOS_ACTIVIDAD.forEach { t -> Chip(t, t == f.tipo) { vm.cambiarTipo(t) } }
            }
            Spacer(Modifier.height(12.dp))
            Campo("Nombre de la actividad", f.nombre, vm::cambiarNombre)
            Spacer(Modifier.height(10.dp))
            Campo("Instrucciones (opcional)", f.descripcion, vm::cambiarDescripcion, lineas = 3)
        }

        Spacer(Modifier.height(12.dp))
        Seccion("¿Dónde?") {
            Etiqueta("Lote")
            if (e.lotes.isEmpty()) {
                Text("Cargando lotes…", fontSize = 12.sp, color = AgroColors.TextMuted)
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    e.lotes.forEach { l -> Chip(l.nombre, l.id == f.loteId) { vm.elegirLote(l.id) } }
                }
            }
            if (f.loteId != null) {
                Spacer(Modifier.height(12.dp))
                Etiqueta("Cultivo")
                when {
                    e.cargandoCultivos -> Text("Cargando cultivos…", fontSize = 12.sp, color = AgroColors.TextMuted)
                    e.cultivos.isEmpty() -> Text("Este lote no tiene cultivos activos.", fontSize = 12.sp, color = AgroColors.Warning)
                    else -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        e.cultivos.forEach { c -> Chip(c.nombreCultivo, c.id == f.cultivoId) { vm.elegirCultivo(c.id) } }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Seccion("Tiempo y costo") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo("Horas estimadas", f.horas, vm::cambiarHoras, KeyboardType.Decimal, Modifier.weight(1f))
                Campo("Valor hora (COP)", f.valorHora, vm::cambiarValorHora, KeyboardType.Number, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))
        Seccion("¿Quién la hace?") {
            if (e.aprendices.isEmpty()) {
                Text("No tienes aprendices a cargo o se están cargando.", fontSize = 12.sp, color = AgroColors.TextMuted)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (f.aprendices.isEmpty()) "Sin aprendices elegidos" else "${f.aprendices.size} elegidos",
                        fontSize = 12.sp,
                        color = AgroColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (f.aprendices.size == e.aprendices.size) "Quitar todos" else "Seleccionar todos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgroColors.Primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.seleccionarTodos() }.padding(6.dp),
                    )
                }
                e.aprendices.forEach { ap ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { vm.alternarAprendiz(ap.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = ap.id in f.aprendices,
                            onCheckedChange = { vm.alternarAprendiz(ap.id) },
                            colors = CheckboxDefaults.colors(checkedColor = AgroColors.Primary),
                        )
                        Text(ap.nombreCompleto, fontSize = 13.sp, color = AgroColors.Text, modifier = Modifier.weight(1f))
                        ap.idFicha?.let { Text(it, fontSize = 10.sp, color = AgroColors.TextMuted) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Etiqueta("Plazo para entregar")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Plazo.entries.forEach { p -> Chip(p.etiqueta, p == f.plazo) { vm.cambiarPlazo(p) } }
                }
            }
        }

        e.errorForm?.let {
            Spacer(Modifier.height(12.dp))
            Franja(it, exito = false)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = vm::guardar,
            enabled = !e.guardando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
        ) {
            if (e.guardando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(if (f.aprendices.isEmpty()) "Crear actividad" else "Crear y asignar", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = vm::cancelarCreacion, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar", color = AgroColors.TextSecondary)
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Title)
            Spacer(Modifier.height(12.dp))
            contenido()
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.TextSecondary)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    lineas: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta, fontSize = 12.sp) },
        singleLine = lineas == 1,
        minLines = lineas,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroColors.Primary,
            unfocusedBorderColor = AgroColors.Border,
            focusedLabelColor = AgroColors.Primary,
            cursorColor = AgroColors.Primary,
        ),
        modifier = modifier,
    )
}
