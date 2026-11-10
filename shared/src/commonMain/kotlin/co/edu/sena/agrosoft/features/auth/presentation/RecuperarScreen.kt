package co.edu.sena.agrosoft.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.theme.AgroColors
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide

@Composable
fun RecuperarScreen(
    correoInicial: String,
    onVolver: () -> Unit,
    onTerminar: (String) -> Unit,
) {
    val vm = viewModel { RecuperarViewModel() }
    val e = vm.estado

    LaunchedEffect(Unit) { vm.reiniciar(correoInicial) }

    val (titulo, subtitulo) = when (e.paso) {
        PasoRecuperar.CORREO -> "Recupera tu contraseña" to "Escribe el correo con el que ingresas y te enviaremos un código."
        PasoRecuperar.CODIGO -> "Revisa tu correo" to "Escribe el código de 6 dígitos que te enviamos a ${e.correo}."
        PasoRecuperar.NUEVA -> "Crea tu nueva contraseña" to "Usa al menos 8 caracteres. Evita contraseñas que uses en otros sitios."
        PasoRecuperar.LISTO -> "¡Listo!" to "Tu contraseña se cambió correctamente."
    }

    MarcoAcceso(
        titulo = titulo,
        subtitulo = subtitulo,
        onVolver = {
            when (e.paso) {
                PasoRecuperar.CODIGO, PasoRecuperar.NUEVA -> vm.volverAlCorreo()
                else -> onVolver()
            }
        },
    ) {
        e.mensaje?.let { AvisoAcceso(it, error = false) }
        e.error?.let { AvisoAcceso(it, error = true) }

        when (e.paso) {
            PasoRecuperar.CORREO -> {
                CampoAcceso("Correo electrónico", e.correo, vm::cambiarCorreo, "tucorreo@ejemplo.com", KeyboardType.Email)
                BotonAcceso("Enviar código", e.cargando, vm::enviarCodigo)
            }
            PasoRecuperar.CODIGO -> {
                CampoAcceso(
                    "Código",
                    e.codigo,
                    vm::cambiarCodigo,
                    "000000",
                    KeyboardType.NumberPassword,
                    tamano = 24.sp,
                    espaciado = 8.sp,
                )
                BotonAcceso("Verificar código", e.cargando, vm::verificar, habilitado = e.codigo.length == 6)
                EnlaceAcceso("¿No te llegó? Reenviar código", vm::reenviar)
            }
            PasoRecuperar.NUEVA -> {
                CampoAcceso("Nueva contraseña", e.nueva, vm::cambiarNueva, "Mínimo 8 caracteres", contrasena = true)
                CampoAcceso("Confirmar contraseña", e.confirmar, vm::cambiarConfirmar, "Repite la contraseña", contrasena = true)
                BotonAcceso("Guardar contraseña", e.cargando, vm::guardar)
            }
            PasoRecuperar.LISTO -> {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(AgroColors.SuccessSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Lucide.CircleCheck, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Ya puedes ingresar con tu nueva contraseña.",
                        fontSize = 14.sp,
                        color = AgroColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                }
                BotonAcceso("Ir a iniciar sesión", false, { onTerminar(e.correo) })
            }
        }
    }
}
