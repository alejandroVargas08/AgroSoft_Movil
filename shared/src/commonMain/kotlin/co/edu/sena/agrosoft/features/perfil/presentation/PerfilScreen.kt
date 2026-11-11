package co.edu.sena.agrosoft.features.perfil.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.features.auth.data.Sesion
import co.edu.sena.agrosoft.features.perfil.data.Perfil
import com.composables.icons.lucide.*

@Composable
fun PerfilScreen(
    sesion: Sesion,
    onActualizado: (Perfil) -> Unit,
    onNotificaciones: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val vm = viewModel(key = "perfil-${sesion.id}") { PerfilViewModel(sesion.id) }
    val e = vm.estado

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Encabezado(sesion, e.perfil)

        e.aviso?.let {
            Spacer(Modifier.height(12.dp))
            Aviso(it, AgroColors.SuccessSoft, AgroColors.Primary, Lucide.CircleCheck)
        }
        e.error?.let {
            Spacer(Modifier.height(12.dp))
            Aviso(it, AgroColors.ErrorSoft, AgroColors.Error, Lucide.CircleAlert)
        }

        when {
            e.cargando -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            }
            e.perfil != null -> {
                if (e.editando) {
                    TituloSeccion("Editar perfil")
                    FormularioEdicion(e, vm, onActualizado)
                } else {
                    TituloSeccion("Datos personales")
                    DatosPersonales(e.perfil)
                }

                TituloSeccion("Cuenta")
                Tarjeta(modifier = Modifier.fillMaxWidth()) {
                    if (!e.editando) {
                        Opcion(Lucide.UserPen, "Editar perfil", onClick = vm::empezarEdicion)
                        Separador()
                    }
                    Opcion(Lucide.Bell, "Notificaciones", onClick = onNotificaciones)
                    Separador()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconoOpcion(Lucide.ShieldCheck)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Verificación en dos pasos", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AgroColors.Text)
                            Text("Te pedimos un código de tu correo al entrar", fontSize = 10.sp, color = AgroColors.TextSecondary)
                        }
                        Switch(
                            checked = e.perfil.dosPasosActivo,
                            onCheckedChange = vm::cambiarDosPasos,
                            enabled = !e.cambiandoDosPasos,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AgroColors.Primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = AgroColors.Border,
                                uncheckedBorderColor = AgroColors.Border,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = vm::pedirSalida,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Error, contentColor = Color.White),
        ) {
            Icon(Lucide.LogOut, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (e.confirmarSalida) {
        AlertDialog(
            onDismissRequest = vm::cancelarSalida,
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("¿Cerrar sesión?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text) },
            text = { Text("Tendrás que volver a identificarte para entrar a AgroSoft.", fontSize = 14.sp, color = AgroColors.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        vm.cancelarSalida()
                        onCerrarSesion()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Error, contentColor = Color.White),
                ) { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = vm::cancelarSalida,
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Cancelar", color = AgroColors.Text, fontWeight = FontWeight.SemiBold) }
            },
        )
    }
}

@Composable
private fun Encabezado(sesion: Sesion, perfil: Perfil?) {
    val nombre = perfil?.nombreCompleto ?: sesion.nombreCompleto
    val correo = perfil?.correo ?: sesion.correo
    val rol = perfil?.rol ?: sesion.rol.etiqueta
    val programa = perfil?.programaFormacion
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(AgroColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(inicialesDe(nombre), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(nombre, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text, textAlign = TextAlign.Center)
            Text("$rol · $correo", fontSize = 12.sp, color = AgroColors.TextSecondary, textAlign = TextAlign.Center)
            if (programa != null) {
                Text(
                    listOfNotNull(programa.numeroFicha?.let { "Ficha $it" }, programa.nombre).joinToString(" · "),
                    fontSize = 12.sp,
                    color = AgroColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DatosPersonales(p: Perfil) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Dato("Identificación", p.documento ?: "—")
        Separador()
        Dato("Correo", p.correo ?: "—", if (p.correoVerificado) "Verificado" else null)
        Separador()
        Dato("Teléfono", p.telefono?.takeIf { it.isNotBlank() } ?: "Sin registrar")
        Separador()
        Dato("Estado", p.estado?.replaceFirstChar { it.uppercase() } ?: "—")
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, insignia: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, fontSize = 13.sp, color = AgroColors.TextSecondary, modifier = Modifier.width(110.dp))
        Text(
            valor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AgroColors.Text,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (insignia != null) {
            Spacer(Modifier.width(6.dp))
            Icon(Lucide.BadgeCheck, contentDescription = insignia, tint = AgroColors.Primary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun FormularioEdicion(e: PerfilEstado, vm: PerfilViewModel, onActualizado: (Perfil) -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoPerfil("Nombre", e.nombre, vm::cambiarNombre)
            CampoPerfil("Apellido", e.apellido, vm::cambiarApellido)
            CampoPerfil("Teléfono", e.telefono, vm::cambiarTelefono, KeyboardType.Phone)

            e.errorEdicion?.let { Text(it, fontSize = 12.sp, color = AgroColors.Error) }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = vm::cancelarEdicion,
                    enabled = !e.guardando,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Cancelar", color = AgroColors.Text, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { vm.guardar(onActualizado) },
                    enabled = !e.guardando,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
                ) {
                    if (e.guardando) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Guardar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoPerfil(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroColors.Primary,
            unfocusedBorderColor = AgroColors.Border,
            focusedLabelColor = AgroColors.Primary,
            cursorColor = AgroColors.Primary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Opcion(icono: ImageVector, texto: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoOpcion(icono)
        Spacer(Modifier.width(12.dp))
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AgroColors.Text, modifier = Modifier.weight(1f))
        Icon(Lucide.ChevronRight, contentDescription = null, tint = AgroColors.TextSecondary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun IconoOpcion(icono: ImageVector) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AgroColors.PrimarySoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun Separador() {
    HorizontalDivider(color = AgroColors.Border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun Aviso(texto: String, fondo: Color, color: Color, icono: ImageVector) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fondo).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 12.sp, color = color, lineHeight = 16.sp)
    }
}

private fun inicialesDe(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercaseChar() }.joinToString("").ifEmpty { "?" }
