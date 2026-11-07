package co.edu.sena.agrosoft.features.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.auth.data.AuthRepositorio
import co.edu.sena.agrosoft.features.auth.data.ResultadoLogin
import co.edu.sena.agrosoft.features.auth.data.Sesion
import kotlinx.coroutines.launch

data class LoginEstado(
    val verificando: Boolean = true,
    val correo: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,
    val sesion: Sesion? = null,
    val correoCodigo: String? = null,
    val codigo: String = "",
    val mensajeCodigo: String? = null,
    val errorCodigo: String? = null,
)

class LoginViewModel(
    private val repositorio: AuthRepositorio = AuthRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(LoginEstado())
        private set

    init {
        viewModelScope.launch {
            val sesion = repositorio.restaurarSesion()
            estado = estado.copy(verificando = false, sesion = sesion, correo = sesion?.correo.orEmpty())
        }
    }

    fun cambiarCorreo(valor: String) {
        estado = estado.copy(correo = valor, error = null, aviso = null)
    }

    fun cambiarPassword(valor: String) {
        estado = estado.copy(password = valor, error = null, aviso = null)
    }

    fun iniciarSesion() {
        if (estado.cargando) return
        estado = estado.copy(cargando = true, error = null, aviso = null)
        viewModelScope.launch {
            aplicar(repositorio.iniciarSesion(estado.correo, estado.password))
        }
    }

    fun cambiarCodigo(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(6)
        estado = estado.copy(codigo = limpio, errorCodigo = null)
        if (limpio.length == 6) verificarCodigo()
    }

    fun verificarCodigo() {
        val correo = estado.correoCodigo ?: return
        if (estado.cargando) return
        if (estado.codigo.length != 6) {
            estado = estado.copy(errorCodigo = "El código tiene 6 dígitos.")
            return
        }
        estado = estado.copy(cargando = true, errorCodigo = null)
        viewModelScope.launch {
            aplicar(repositorio.verificarCodigo(correo, estado.codigo))
        }
    }

    fun reenviarCodigo() {
        if (estado.cargando) return
        estado = estado.copy(cargando = true, errorCodigo = null, codigo = "")
        viewModelScope.launch {
            when (val r = repositorio.iniciarSesion(estado.correo, estado.password)) {
                is ResultadoLogin.RequiereCodigo -> estado = estado.copy(
                    cargando = false,
                    mensajeCodigo = "Te enviamos un código nuevo a tu correo.",
                )
                else -> aplicar(r)
            }
        }
    }

    fun cancelarCodigo() {
        estado = estado.copy(correoCodigo = null, codigo = "", password = "", mensajeCodigo = null, errorCodigo = null)
    }

    fun cerrarSesion() {
        repositorio.cerrarSesion()
        estado = LoginEstado(verificando = false, correo = estado.correo)
    }

    private fun aplicar(resultado: ResultadoLogin) {
        estado = when (resultado) {
            is ResultadoLogin.Exito -> estado.copy(
                cargando = false,
                password = "",
                codigo = "",
                correoCodigo = null,
                sesion = resultado.sesion,
            )
            is ResultadoLogin.RequiereCodigo -> estado.copy(
                cargando = false,
                correoCodigo = resultado.correo,
                mensajeCodigo = resultado.mensaje,
                codigo = "",
                errorCodigo = null,
            )
            is ResultadoLogin.AvisoCuenta -> estado.copy(
                cargando = false,
                correoCodigo = null,
                codigo = "",
                aviso = resultado.mensaje,
            )
            is ResultadoLogin.Error -> if (estado.correoCodigo != null) {
                estado.copy(cargando = false, codigo = "", errorCodigo = resultado.mensaje)
            } else {
                estado.copy(cargando = false, error = resultado.mensaje)
            }
        }
    }
}
