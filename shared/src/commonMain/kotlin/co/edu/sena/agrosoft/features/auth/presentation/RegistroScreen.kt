package co.edu.sena.agrosoft.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.features.auth.data.FichaPublica
import com.composables.icons.lucide.*

@Composable
fun RegistroScreen(
    onVolver: () -> Unit,
    onTerminar: (String) -> Unit,
) {
    val vm = viewModel { RegistroViewModel() }
    val e = vm.estado

    LaunchedEffect(Unit) { vm.reiniciar() }

    val (titulo, subtitulo) = when (e.paso) {
        1 -> "Solicitar acceso" to "Cuéntanos cómo vas a usar AgroSoft."
        2 -> "Tus datos" to "Con estos datos te identificaremos en AgroSoft."
        3 -> when (e.tipo) {
            TipoRegistro.APRENDIZ -> "Tu ficha" to "Usa el código que te dio tu instructor o elige tu ficha."
            TipoRegistro.INSTRUCTOR -> "Tus fichas" to "Elige las fichas que tendrás a cargo."
            else -> "Tu solicitud" to "Un administrador revisará tu solicitud."
        }
        else -> if (e.requiereAprobacion == true) "Solicitud enviada" to "Te avisaremos cuando la aprueben."
        else "¡Bienvenido a AgroSoft!" to "Tu cuenta quedó activa."
    }

    MarcoAcceso(
        titulo = titulo,
        subtitulo = subtitulo,
        onVolver = { if (e.paso == 4 || !vm.atras()) onVolver() },
    ) {
        if (e.paso in 1..3) {
            Progreso(e.paso)
            Spacer(Modifier.height(20.dp))
        }
        e.error?.let { AvisoAcceso(it, error = true) }

        when (e.paso) {
            1 -> {
                TipoRegistro.entries.forEach { t ->
                    OpcionTipo(
                        t,
                        when (t) {
                            TipoRegistro.APRENDIZ -> Lucide.GraduationCap
                            TipoRegistro.INSTRUCTOR -> Lucide.Users
                            TipoRegistro.ADMINISTRADOR -> Lucide.ShieldCheck
                        },
                        seleccionado = e.tipo == t,
                    ) { vm.elegirTipo(t) }
                }
                Spacer(Modifier.height(8.dp))
                BotonAcceso("Continuar", false, vm::siguiente, habilitado = e.tipo != null)
            }
            2 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoAcceso("Nombre", e.nombre, vm::cambiarNombre, "Juan", modifier = Modifier.weight(1f))
                    CampoAcceso("Apellido", e.apellido, vm::cambiarApellido, "Pérez", modifier = Modifier.weight(1f))
                }
                CampoAcceso("Identificación", e.identificacion, vm::cambiarIdentificacion, "Número de documento", KeyboardType.Number)
                CampoAcceso("Teléfono", e.telefono, vm::cambiarTelefono, "3001234567", KeyboardType.Phone)
                CampoAcceso("Correo electrónico", e.correo, vm::cambiarCorreo, "tucorreo@ejemplo.com", KeyboardType.Email)
                CampoAcceso("Contraseña", e.password, vm::cambiarPassword, "Mínimo 8 caracteres", contrasena = true)
                CampoAcceso("Confirmar contraseña", e.confirmar, vm::cambiarConfirmar, "Repite la contraseña", contrasena = true)
                BotonAcceso("Continuar", false, vm::siguiente)
            }
            3 -> {
                when (e.tipo) {
                    TipoRegistro.APRENDIZ -> PasoAprendiz(e, vm)
                    TipoRegistro.INSTRUCTOR -> PasoInstructor(e, vm)
                    TipoRegistro.ADMINISTRADOR -> {
                        CampoAcceso("Cargo", e.cargo, vm::cambiarCargo, "Ej: Coordinador de la unidad productiva")
                        CampoAcceso("¿Por qué necesitas acceso de administrador?", e.motivo, vm::cambiarMotivo, "Cuéntanos brevemente", lineas = 4)
                    }
                    null -> {}
                }
                Spacer(Modifier.height(4.dp))
                BotonAcceso(
                    when {
                        e.tipo == TipoRegistro.ADMINISTRADOR -> "Enviar solicitud"
                        e.tipo == TipoRegistro.INSTRUCTOR && e.codigoInstructor.isBlank() -> "Enviar solicitud"
                        else -> "Crear cuenta"
                    },
                    e.enviando,
                    vm::enviar,
                )
            }
            else -> Resultado(e, onTerminar)
        }
    }
}

@Composable
private fun Progreso(paso: Int) {
    Column {
        Text("Paso $paso de 3", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.TextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..3).forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (i <= paso) AgroColors.Primary else AgroColors.Border),
                )
            }
        }
    }
}

@Composable
private fun OpcionTipo(t: TipoRegistro, icono: ImageVector, seleccionado: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(forma)
            .background(if (seleccionado) AgroColors.PrimarySoft else Color.White)
            .border(if (seleccionado) 2.dp else 1.dp, if (seleccionado) AgroColors.Primary else AgroColors.Border, forma)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (seleccionado) AgroColors.Primary else AgroColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = if (seleccionado) Color.White else AgroColors.Primary, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Text(t.detalle, fontSize = 12.sp, color = AgroColors.TextSecondary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PasoAprendiz(e: RegistroEstado, vm: RegistroViewModel) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F0EA)).padding(4.dp),
    ) {
        listOf(true to "Tengo un código", false to "Elegir mi ficha").forEach { (valor, texto) ->
            val activo = e.usarCodigo == valor
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activo) Color.White else Color.Transparent)
                    .clickable { vm.usarCodigo(valor) },
                contentAlignment = Alignment.Center,
            ) {
                Text(texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (activo) AgroColors.Primary else AgroColors.TextSecondary)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    if (e.usarCodigo) {
        CampoAcceso(
            "Código de invitación",
            e.codigoInvitacion,
            vm::cambiarCodigoInvitacion,
            "Ej: A7K2P9",
            tamano = 18.sp,
            espaciado = 3.sp,
        )
        Text(
            "Tu instructor lo genera desde la web, en Mis fichas. Con él quedas en la ficha correcta.",
            fontSize = 12.sp,
            color = AgroColors.TextMuted,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
    } else {
        ListaFichas(e, vm, multiple = false)
    }
}

@Composable
private fun PasoInstructor(e: RegistroEstado, vm: RegistroViewModel) {
    ListaFichas(e, vm, multiple = true)
    Spacer(Modifier.height(6.dp))
    CampoAcceso("Código de instructor (opcional)", e.codigoInstructor, vm::cambiarCodigoInstructor, "Si te lo dieron, escríbelo aquí")
    Text(
        if (e.codigoInstructor.isBlank()) "Sin código, tu solicitud la aprueba un administrador."
        else "Con un código válido tu cuenta queda activa de inmediato.",
        fontSize = 12.sp,
        color = AgroColors.TextMuted,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

@Composable
private fun ListaFichas(e: RegistroEstado, vm: RegistroViewModel, multiple: Boolean) {
    CampoAcceso("Buscar ficha", e.busqueda, vm::cambiarBusqueda, "Número o nombre del programa")
    when {
        e.cargandoFichas -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AgroColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
        e.fichas.isEmpty() -> Text(
            "No hay fichas disponibles.",
            fontSize = 13.sp,
            color = AgroColors.TextMuted,
            modifier = Modifier.padding(bottom = 14.dp).clickable { vm.cargarFichas() },
        )
        else -> Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, AgroColors.Border, RoundedCornerShape(14.dp)),
        ) {
            val lista = e.fichasFiltradas.take(30)
            if (lista.isEmpty()) {
                Text("Ninguna ficha coincide.", fontSize = 13.sp, color = AgroColors.TextMuted, modifier = Modifier.padding(16.dp))
            }
            lista.forEach { f ->
                FilaFicha(
                    f,
                    seleccionada = if (multiple) f.numero in e.fichasACargo else e.fichaId == f.id,
                    multiple = multiple,
                ) {
                    if (multiple) vm.alternarFichaACargo(f.numero) else vm.elegirFicha(f.id)
                }
            }
        }
    }
}

@Composable
private fun FilaFicha(f: FichaPublica, seleccionada: Boolean, multiple: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (seleccionada) AgroColors.PrimarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiple) {
            Checkbox(checked = seleccionada, onCheckedChange = { onClick() }, colors = CheckboxDefaults.colors(checkedColor = AgroColors.Primary))
        } else {
            RadioButton(selected = seleccionada, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = AgroColors.Primary))
        }
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text("Ficha ${f.numero}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Text(
                listOfNotNull(f.nombre.takeIf { it.isNotBlank() }, f.tipo).joinToString(" · "),
                fontSize = 11.sp,
                color = AgroColors.TextSecondary,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun Resultado(e: RegistroEstado, onTerminar: (String) -> Unit) {
    val pendiente = e.requiereAprobacion == true
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(if (pendiente) AgroColors.AccentSoft else AgroColors.SuccessSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (pendiente) Lucide.Clock else Lucide.CircleCheck,
                contentDescription = null,
                tint = if (pendiente) AgroColors.Accent else AgroColors.Primary,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (pendiente) {
                "Tu solicitud como ${e.tipo?.titulo?.lowercase()} quedó en revisión. Te llegará un aviso cuando un administrador la apruebe. Mientras tanto no podrás ingresar."
            } else {
                "Ya puedes ingresar con ${e.correo} y la contraseña que creaste."
            },
            fontSize = 14.sp,
            color = AgroColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
    BotonAcceso(if (pendiente) "Volver al inicio" else "Ir a iniciar sesión", false, { onTerminar(e.correo) })
}
