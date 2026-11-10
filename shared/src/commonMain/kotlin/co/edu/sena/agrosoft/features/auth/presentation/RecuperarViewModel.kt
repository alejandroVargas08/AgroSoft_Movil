package co.edu.sena.agrosoft.features.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.auth.data.AccesoRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.launch

enum class PasoRecuperar { CORREO, CODIGO, NUEVA, LISTO }

data class RecuperarEstado(
    val paso: PasoRecuperar = PasoRecuperar.CORREO,
    val correo: String = "",
    val codigo: String = "",
    val nueva: String = "",
    val confirmar: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
)

class RecuperarViewModel(
    private val repositorio: AccesoRepositorio = AccesoRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(RecuperarEstado())
        private set

    fun reiniciar(correo: String = "") {
        estado = RecuperarEstado(correo = correo)
    }

    fun cambiarCorreo(v: String) { estado = estado.copy(correo = v.trim(), error = null) }
    fun cambiarNueva(v: String) { estado = estado.copy(nueva = v, error = null) }
    fun cambiarConfirmar(v: String) { estado = estado.copy(confirmar = v, error = null) }

    fun cambiarCodigo(v: String) {
        val limpio = v.filter { it.isDigit() }.take(6)
        estado = estado.copy(codigo = limpio, error = null)
        if (limpio.length == 6) verificar()
    }

    fun enviarCodigo() {
        if (!correoValido(estado.correo)) {
            estado = estado.copy(error = "Escribe un correo válido.")
            return
        }
        estado = estado.copy(cargando = true, error = null, mensaje = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.enviarCodigo(estado.correo)) {
                is Resultado.Ok -> estado.copy(
                    cargando = false,
                    paso = PasoRecuperar.CODIGO,
                    codigo = "",
                    mensaje = "Si ${estado.correo} está registrado, te llegará un código de 6 dígitos. Revisa también la carpeta de spam.",
                )
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun reenviar() {
        estado = estado.copy(codigo = "")
        enviarCodigo()
    }

    fun verificar() {
        if (estado.codigo.length != 6 || estado.cargando) return
        estado = estado.copy(cargando = true, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.verificarCodigo(estado.correo, estado.codigo)) {
                is Resultado.Ok -> estado.copy(cargando = false, paso = PasoRecuperar.NUEVA, mensaje = null)
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje, codigo = "")
            }
        }
    }

    fun guardar() {
        val e = estado
        val error = when {
            e.nueva.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
            e.nueva != e.confirmar -> "Las contraseñas no coinciden."
            else -> null
        }
        if (error != null) {
            estado = e.copy(error = error)
            return
        }
        estado = e.copy(cargando = true, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.restablecer(e.correo, e.codigo, e.nueva)) {
                is Resultado.Ok -> estado.copy(cargando = false, paso = PasoRecuperar.LISTO, nueva = "", confirmar = "")
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun volverAlCorreo() {
        estado = estado.copy(paso = PasoRecuperar.CORREO, codigo = "", error = null, mensaje = null)
    }
}
