package co.edu.sena.agrosoft.features.admin.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.util.Fechas
import co.edu.sena.agrosoft.features.admin.data.UsuarioAdmin
import com.composables.icons.lucide.*

@Composable
fun SolicitudesScreen(vm: AdminViewModel) {
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        e.aviso?.let { Banda(it, AgroColors.SuccessSoft, AgroColors.Primary) }
        e.errorAccion?.let { Banda(it, AgroColors.ErrorSoft, AgroColors.Error) }

        when {
            e.cargando -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
            e.solicitudes.isEmpty() -> MensajeAdmin(Lucide.CircleCheck, "No hay solicitudes pendientes.")
            else -> {
                Text(
                    if (e.solicitudes.size == 1) "1 persona espera aprobación" else "${e.solicitudes.size} personas esperan aprobación",
                    fontSize = 12.sp,
                    color = AgroColors.TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    e.solicitudes.forEach { u -> TarjetaSolicitud(u, e, vm) }
                }
            }
        }
    }

    val rechazando = e.rechazando
    if (rechazando != null) {
        AlertDialog(
            onDismissRequest = vm::cancelarRechazo,
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Rechazar solicitud", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text) },
            text = {
                Column {
                    Text(
                        "${rechazando.nombreCompleto} verá este motivo.",
                        fontSize = 13.sp,
                        color = AgroColors.TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = e.motivo,
                        onValueChange = vm::cambiarMotivo,
                        label = { Text("Motivo", fontSize = 12.sp) },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgroColors.Error,
                            focusedLabelColor = AgroColors.Error,
                            cursorColor = AgroColors.Error,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = vm::confirmarRechazo,
                    enabled = e.motivo.trim().length >= 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Error),
                ) { Text("Rechazar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(onClick = vm::cancelarRechazo, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = AgroColors.Text, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun TarjetaSolicitud(u: UsuarioAdmin, e: AdminEstado, vm: AdminViewModel) {
    val s = u.solicitud
    val ocupado = e.procesandoId == u.id
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(AgroColors.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        u.nombreCompleto.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroColors.Primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.nombreCompleto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
                    Text(u.correo.orEmpty(), fontSize = 11.sp, color = AgroColors.TextSecondary)
                }
                s?.rolSolicitado?.let {
                    Text(
                        it,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.clip(CircleShape).background(AgroColors.Chip).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AgroColors.Background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                u.documento?.let { Linea(Lucide.IdCard, "Documento $it") }
                u.telefono?.takeIf { it.isNotBlank() }?.let { Linea(Lucide.Phone, it) }
                s?.cargo?.takeIf { it.isNotBlank() }?.let { Linea(Lucide.Briefcase, it) }
                s?.fichasACargo?.takeIf { it.isNotEmpty() }?.let { ids ->
                    Linea(Lucide.GraduationCap, ids.joinToString(", ") { e.nombreFicha(it) })
                }
                Linea(Lucide.Clock, "Solicitó ${(Fechas.haceCuanto(s?.solicitadoEn ?: u.creadoEn) ?: "hace un tiempo").lowercase()}")
            }
            s?.motivo?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text("\"$it\"", fontSize = 12.sp, color = AgroColors.TextSecondary, lineHeight = 17.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { vm.pedirRechazo(u) },
                    enabled = !ocupado,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AgroColors.Error),
                ) { Text("Rechazar", color = AgroColors.Error, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { vm.aprobar(u) },
                    enabled = !ocupado,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
                ) {
                    if (ocupado) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Aprobar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Linea(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 12.sp, color = AgroColors.Text)
    }
}

@Composable
internal fun Banda(texto: String, fondo: Color, color: Color) {
    Text(
        texto,
        fontSize = 12.sp,
        color = color,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(12.dp)).background(fondo).padding(12.dp),
    )
}
