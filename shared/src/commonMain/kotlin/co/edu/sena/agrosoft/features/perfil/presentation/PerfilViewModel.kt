package co.edu.sena.agrosoft.features.perfil.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.perfil.data.CambiosPerfil
import co.edu.sena.agrosoft.features.perfil.data.Perfil
import co.edu.sena.agrosoft.features.perfil.data.PerfilRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.launch

data class PerfilEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val perfil: Perfil? = null,
    val editando: Boolean = false,
    val nombre: String = "",
    val apellido: String = "",
    val telefono: String = "",
    val guardando: Boolean = false,
    val errorEdicion: String? = null,
    val aviso: String? = null,
    val cambiandoDosPasos: Boolean = false,
    val confirmarSalida: Boolean = false,
)

class PerfilViewModel(
    private val usuarioId: Int,
    private val repositorio: PerfilRepositorio = PerfilRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(PerfilEstado())
        private set

    fun cargar() {
        estado = estado.copy(cargando = estado.perfil == null, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.obtener(usuarioId)) {
                is Resultado.Ok -> estado.copy(cargando = false, perfil = r.dato)
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun empezarEdicion() {
        val p = estado.perfil ?: return
        estado = estado.copy(
            editando = true,
            nombre = p.nombre,
            apellido = p.apellido,
            telefono = p.telefono.orEmpty(),
            errorEdicion = null,
            aviso = null,
        )
    }

    fun cancelarEdicion() {
        estado = estado.copy(editando = false, errorEdicion = null)
    }

    fun cambiarNombre(v: String) { estado = estado.copy(nombre = v, errorEdicion = null) }
    fun cambiarApellido(v: String) { estado = estado.copy(apellido = v, errorEdicion = null) }
    fun cambiarTelefono(v: String) { estado = estado.copy(telefono = v.filter { it.isDigit() || it == '+' || it == ' ' }, errorEdicion = null) }

    fun guardar(onGuardado: (Perfil) -> Unit) {
        val nombre = estado.nombre.trim()
        val apellido = estado.apellido.trim()
        if (nombre.length < 2) {
            estado = estado.copy(errorEdicion = "Escribe tu nombre.")
            return
        }
        if (apellido.length < 2) {
            estado = estado.copy(errorEdicion = "Escribe tu apellido.")
            return
        }
        estado = estado.copy(guardando = true, errorEdicion = null)
        viewModelScope.launch {
            val cambios = CambiosPerfil(nombre = nombre, apellido = apellido, telefono = estado.telefono.trim())
            when (val r = repositorio.actualizar(usuarioId, cambios)) {
                is Resultado.Ok -> {
                    val actualizado = (repositorio.obtener(usuarioId) as? Resultado.Ok)?.dato
                        ?: estado.perfil?.copy(nombre = nombre, apellido = apellido, telefono = cambios.telefono)
                    estado = estado.copy(guardando = false, editando = false, perfil = actualizado, aviso = "Tus datos se actualizaron.")
                    actualizado?.let(onGuardado)
                }
                is Resultado.Falla -> estado = estado.copy(guardando = false, errorEdicion = r.mensaje)
            }
        }
    }

    fun cambiarDosPasos(activo: Boolean) {
        val p = estado.perfil ?: return
        if (estado.cambiandoDosPasos) return
        estado = estado.copy(cambiandoDosPasos = true, perfil = p.copy(dosPasosActivo = activo), aviso = null, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.actualizar(usuarioId, CambiosPerfil(dosPasosActivo = activo))) {
                is Resultado.Ok -> estado.copy(
                    cambiandoDosPasos = false,
                    aviso = if (activo) "Verificación en dos pasos activada. Al entrar te pediremos un código de tu correo."
                    else "Verificación en dos pasos desactivada.",
                )
                is Resultado.Falla -> estado.copy(
                    cambiandoDosPasos = false,
                    perfil = p,
                    error = r.mensaje,
                )
            }
        }
    }

    fun pedirSalida() { estado = estado.copy(confirmarSalida = true) }
    fun cancelarSalida() { estado = estado.copy(confirmarSalida = false) }
}
