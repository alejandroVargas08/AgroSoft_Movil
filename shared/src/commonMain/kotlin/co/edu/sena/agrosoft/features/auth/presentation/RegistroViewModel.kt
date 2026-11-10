package co.edu.sena.agrosoft.features.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.auth.data.AccesoRepositorio
import co.edu.sena.agrosoft.features.auth.data.FichaPublica
import co.edu.sena.agrosoft.features.auth.data.SolicitudRegistroDto
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.launch

enum class TipoRegistro(val valor: String, val titulo: String, val detalle: String) {
    APRENDIZ("aprendiz", "Aprendiz", "Entras de una vez con tu ficha."),
    INSTRUCTOR("instructor", "Instructor", "Con código de instructor entras de una vez. Sin código, un administrador aprueba tu solicitud."),
    ADMINISTRADOR("administrador", "Administrador", "Otro administrador debe aprobar tu solicitud."),
}

data class RegistroEstado(
    val paso: Int = 1,
    val tipo: TipoRegistro? = null,
    val nombre: String = "",
    val apellido: String = "",
    val identificacion: String = "",
    val telefono: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmar: String = "",
    val usarCodigo: Boolean = true,
    val codigoInvitacion: String = "",
    val fichaId: Int? = null,
    val fichasACargo: Set<String> = emptySet(),
    val codigoInstructor: String = "",
    val cargo: String = "",
    val motivo: String = "",
    val busqueda: String = "",
    val fichas: List<FichaPublica> = emptyList(),
    val cargandoFichas: Boolean = false,
    val enviando: Boolean = false,
    val error: String? = null,
    val requiereAprobacion: Boolean? = null,
) {
    val fichasFiltradas: List<FichaPublica>
        get() {
            val q = busqueda.trim().lowercase()
            return if (q.isEmpty()) fichas else fichas.filter { q in it.numero.lowercase() || q in it.nombre.lowercase() }
        }
}

class RegistroViewModel(
    private val repositorio: AccesoRepositorio = AccesoRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(RegistroEstado())
        private set

    fun reiniciar() {
        estado = RegistroEstado()
    }

    private fun editar(cambio: (RegistroEstado) -> RegistroEstado) {
        estado = cambio(estado).copy(error = null)
    }

    fun elegirTipo(t: TipoRegistro) = editar { it.copy(tipo = t) }
    fun cambiarNombre(v: String) = editar { it.copy(nombre = v) }
    fun cambiarApellido(v: String) = editar { it.copy(apellido = v) }
    fun cambiarIdentificacion(v: String) = editar { it.copy(identificacion = v.filter { c -> c.isLetterOrDigit() }.take(20)) }
    fun cambiarTelefono(v: String) = editar { it.copy(telefono = v.filter { c -> c.isDigit() || c == '+' }.take(15)) }
    fun cambiarCorreo(v: String) = editar { it.copy(correo = v.trim()) }
    fun cambiarPassword(v: String) = editar { it.copy(password = v) }
    fun cambiarConfirmar(v: String) = editar { it.copy(confirmar = v) }
    fun usarCodigo(v: Boolean) = editar { it.copy(usarCodigo = v) }
    fun cambiarCodigoInvitacion(v: String) = editar { it.copy(codigoInvitacion = v.trim().uppercase().take(20)) }
    fun elegirFicha(id: Int) = editar { it.copy(fichaId = id) }
    fun cambiarBusqueda(v: String) = editar { it.copy(busqueda = v) }
    fun cambiarCodigoInstructor(v: String) = editar { it.copy(codigoInstructor = v.trim().take(60)) }
    fun cambiarCargo(v: String) = editar { it.copy(cargo = v.take(120)) }
    fun cambiarMotivo(v: String) = editar { it.copy(motivo = v.take(500)) }

    fun alternarFichaACargo(numero: String) = editar {
        it.copy(fichasACargo = if (numero in it.fichasACargo) it.fichasACargo - numero else it.fichasACargo + numero)
    }

    fun siguiente() {
        val e = estado
        val error = when (e.paso) {
            1 -> if (e.tipo == null) "Elige cómo vas a usar AgroSoft." else null
            2 -> when {
                e.nombre.trim().length < 2 -> "Escribe tu nombre."
                e.apellido.trim().length < 2 -> "Escribe tu apellido."
                e.identificacion.length < 5 -> "Escribe tu número de identificación."
                e.telefono.length < 7 -> "Escribe un teléfono válido."
                !correoValido(e.correo) -> "Escribe un correo válido."
                e.password.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                e.password != e.confirmar -> "Las contraseñas no coinciden."
                else -> null
            }
            else -> null
        }
        if (error != null) {
            estado = e.copy(error = error)
            return
        }
        estado = e.copy(paso = e.paso + 1, error = null)
        if (estado.paso == 3 && e.tipo != TipoRegistro.ADMINISTRADOR && e.fichas.isEmpty()) cargarFichas()
    }

    fun atras(): Boolean {
        if (estado.paso <= 1) return false
        estado = estado.copy(paso = estado.paso - 1, error = null)
        return true
    }

    fun cargarFichas() {
        estado = estado.copy(cargandoFichas = true)
        viewModelScope.launch {
            estado = when (val r = repositorio.fichas()) {
                is Resultado.Ok -> estado.copy(cargandoFichas = false, fichas = r.dato.sortedBy { it.numero })
                is Resultado.Falla -> estado.copy(cargandoFichas = false, error = r.mensaje)
            }
        }
    }

    fun enviar() {
        val e = estado
        val tipo = e.tipo ?: return
        val error = when (tipo) {
            TipoRegistro.APRENDIZ ->
                if (e.usarCodigo && e.codigoInvitacion.length < 4) "Escribe el código de invitación que te dio tu instructor."
                else if (!e.usarCodigo && e.fichaId == null) "Elige tu ficha."
                else null
            TipoRegistro.INSTRUCTOR -> if (e.fichasACargo.isEmpty()) "Elige al menos una ficha a tu cargo." else null
            TipoRegistro.ADMINISTRADOR -> when {
                e.cargo.trim().length < 3 -> "Escribe tu cargo."
                e.motivo.trim().length < 10 -> "Cuéntanos brevemente por qué necesitas acceso de administrador."
                else -> null
            }
        }
        if (error != null) {
            estado = e.copy(error = error)
            return
        }

        val dto = SolicitudRegistroDto(
            nombre = e.nombre.trim(),
            apellido = e.apellido.trim(),
            identificacion = e.identificacion,
            telefono = e.telefono,
            email = e.correo,
            password = e.password,
            tipoUsuario = tipo.valor,
            programaFormacionId = if (tipo == TipoRegistro.APRENDIZ && !e.usarCodigo) e.fichaId else null,
            codigoInvitacion = if (tipo == TipoRegistro.APRENDIZ && e.usarCodigo) e.codigoInvitacion else null,
            codigoInstructor = if (tipo == TipoRegistro.INSTRUCTOR) e.codigoInstructor.ifBlank { null } else null,
            fichasACargo = if (tipo == TipoRegistro.INSTRUCTOR) e.fichasACargo.toList() else null,
            cargo = if (tipo == TipoRegistro.ADMINISTRADOR) e.cargo.trim() else null,
            motivoSolicitud = if (tipo == TipoRegistro.ADMINISTRADOR) e.motivo.trim() else null,
        )

        estado = e.copy(enviando = true, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.registrar(dto)) {
                is Resultado.Ok -> estado.copy(enviando = false, paso = 4, requiereAprobacion = r.dato, password = "", confirmar = "")
                is Resultado.Falla -> estado.copy(enviando = false, error = r.mensaje)
            }
        }
    }
}
