package co.edu.sena.agrosoft.features.mas.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.seguridad.AccesoBiometrico
import co.edu.sena.agrosoft.core.seguridad.Credenciales
import co.edu.sena.agrosoft.core.seguridad.TipoBiometria
import co.edu.sena.agrosoft.core.seguridad.autenticarBiometria
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.features.auth.data.Sesion
import co.edu.sena.agrosoft.features.perfil.data.PerfilRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import kotlinx.coroutines.launch

data class BiometriaEstado(
    val tipo: TipoBiometria? = null,
    val activa: Boolean = false,
    val pidiendoClave: Boolean = false,
    val clave: String = "",
    val error: String? = null,
    val procesando: Boolean = false,
)

class BiometriaViewModel(
    private val usuarioId: Int,
    private val correoSesion: String,
    private val repositorio: PerfilRepositorio = PerfilRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(BiometriaEstado())
        private set

    fun refrescar() {
        estado = estado.copy(tipo = AccesoBiometrico.tipo, activa = AccesoBiometrico.activo)
    }

    fun cambiar(activar: Boolean) {
        if (estado.tipo == null || estado.procesando) return
        if (activar) {
            estado = estado.copy(pidiendoClave = true, clave = "", error = null)
        } else {
            AccesoBiometrico.desactivar()
            AccesoBiometrico.rechazado = true
            estado = estado.copy(activa = false)
        }
    }

    fun cambiarClave(v: String) {
        estado = estado.copy(clave = v, error = null)
    }

    fun cancelar() {
        if (estado.procesando) return
        estado = estado.copy(pidiendoClave = false, clave = "", error = null)
    }

    fun confirmar() {
        val tipo = estado.tipo ?: return
        val clave = estado.clave
        if (clave.length < 6) {
            estado = estado.copy(error = "Escribe tu contraseña.")
            return
        }
        estado = estado.copy(procesando = true, error = null)
        viewModelScope.launch {
            val perfil = (repositorio.obtener(usuarioId) as? Resultado.Ok)?.dato
            val correo = perfil?.correo ?: correoSesion
            if (perfil?.dosPasosActivo != true) {
                val r = repositorio.verificarContrasena(correo, clave)
                if (r is Resultado.Falla) {
                    estado = estado.copy(procesando = false, error = r.mensaje)
                    return@launch
                }
            }
            if (autenticarBiometria("Confirma para activar ${tipo.nombre}")) {
                AccesoBiometrico.activar(Credenciales(correo, clave))
                estado = estado.copy(procesando = false, pidiendoClave = false, clave = "", activa = true)
            } else {
                estado = estado.copy(procesando = false, error = "No se pudo confirmar con ${tipo.nombre}. Intenta de nuevo.")
            }
        }
    }
}

@Composable
fun TarjetaBiometria(sesion: Sesion, modifier: Modifier = Modifier) {
    val vm = viewModel(key = "biometria-${sesion.id}") { BiometriaViewModel(sesion.id, sesion.correo) }
    val e = vm.estado

    LaunchedEffect(Unit) { vm.refrescar() }

    val disponible = e.tipo != null
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), forma)
            .clickable(enabled = disponible && !e.procesando) { vm.cambiar(!e.activa) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Smartphone, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Biometría", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            Text(
                when {
                    !disponible -> "No está configurada en este teléfono"
                    e.activa -> "Acceso rápido activado"
                    else -> "Entra con ${e.tipo?.nombre} sin escribir tu contraseña"
                },
                fontSize = 10.sp,
                color = Color(0xFF6B7280),
            )
        }
        Spacer(Modifier.width(8.dp))
        Interruptor(activo = e.activa, habilitado = disponible)
    }

    if (e.pidiendoClave) {
        DialogoClave(e, vm)
    }
}

@Composable
private fun Interruptor(activo: Boolean, habilitado: Boolean) {
    val fondo by animateColorAsState(if (activo) AgroColors.Primary else Color(0xFFD1D5DB), label = "fondo")
    val desplazamiento by animateDpAsState(if (activo) 20.dp else 0.dp, label = "pulgar")
    Box(
        Modifier
            .size(width = 44.dp, height = 24.dp)
            .alpha(if (habilitado) 1f else 0.5f)
            .clip(CircleShape)
            .background(fondo)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = desplazamiento)
                .size(16.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun DialogoClave(e: BiometriaEstado, vm: BiometriaViewModel) {
    val nombre = e.tipo?.nombre ?: "biometría"
    AlertDialog(
        onDismissRequest = vm::cancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Activar $nombre", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text) },
        text = {
            Column {
                Text(
                    "Escribe tu contraseña. Se guarda cifrada en este dispositivo y solo se usa cuando $nombre te reconoce.",
                    fontSize = 13.sp,
                    color = AgroColors.TextSecondary,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = e.clave,
                    onValueChange = vm::cambiarClave,
                    label = { Text("Contraseña", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = e.error != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgroColors.Primary,
                        unfocusedBorderColor = AgroColors.Border,
                        focusedLabelColor = AgroColors.Primary,
                        cursorColor = AgroColors.Primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                e.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = AgroColors.Error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = vm::confirmar,
                enabled = !e.procesando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
            ) {
                if (e.procesando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("Activar", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = vm::cancelar, enabled = !e.procesando) {
                Text("Cancelar", color = AgroColors.TextSecondary)
            }
        },
    )
}
