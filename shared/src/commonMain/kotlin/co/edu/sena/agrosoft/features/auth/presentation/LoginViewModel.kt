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
    val correo: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,
    val sesion: Sesion? = null,
)

class LoginViewModel(
    private val repositorio: AuthRepositorio = AuthRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(LoginEstado())
        private set

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
            estado = when (val r = repositorio.iniciarSesion(estado.correo, estado.password)) {
                is ResultadoLogin.Exito -> estado.copy(cargando = false, password = "", sesion = r.sesion)
                is ResultadoLogin.AvisoCuenta -> estado.copy(cargando = false, aviso = r.mensaje)
                is ResultadoLogin.Error -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun cerrarSesion() {
        repositorio.cerrarSesion()
        estado = LoginEstado(correo = estado.correo)
    }
}
