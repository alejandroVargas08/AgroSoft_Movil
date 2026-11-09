package co.edu.sena.agrosoft.features.admin.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.admin.data.AdminRepositorio
import co.edu.sena.agrosoft.features.admin.data.FichaResumen
import co.edu.sena.agrosoft.features.admin.data.InsumoBajo
import co.edu.sena.agrosoft.features.admin.data.UsuarioAdmin
import co.edu.sena.agrosoft.features.sensores.data.Sensor
import co.edu.sena.agrosoft.features.sensores.data.SensoresRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class AdminEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val usuarios: List<UsuarioAdmin> = emptyList(),
    val solicitudes: List<UsuarioAdmin> = emptyList(),
    val fichas: List<FichaResumen> = emptyList(),
    val insumos: List<InsumoBajo> = emptyList(),
    val sensores: List<Sensor> = emptyList(),
    val procesandoId: Int? = null,
    val aviso: String? = null,
    val errorAccion: String? = null,
    val rechazando: UsuarioAdmin? = null,
    val motivo: String = "",
) {
    val activos: Int get() = usuarios.count { it.estado?.lowercase() == "activo" }
    val fichasActivas: Int get() = fichas.count { it.activa }
    val sensoresEnAlerta: List<Sensor> get() = sensores.filter { it.enAlerta }
    val totalAlertas: Int get() = sensoresEnAlerta.size + insumos.size

    fun nombreFicha(id: Int): String {
        val f = fichas.firstOrNull { it.id == id } ?: return "Ficha #$id"
        return listOfNotNull(f.numero?.let { "Ficha $it" }, f.nombre.takeIf { it.isNotBlank() }).joinToString(" · ")
    }
}

class AdminViewModel(
    private val repositorio: AdminRepositorio = AdminRepositorio(),
    private val sensores: SensoresRepositorio = SensoresRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(AdminEstado())
        private set

    private var cargandoAhora = false

    fun cargar() {
        if (cargandoAhora) return
        cargandoAhora = true
        estado = estado.copy(cargando = estado.usuarios.isEmpty() && estado.solicitudes.isEmpty(), error = null)
        viewModelScope.launch {
            try {
                coroutineScope {
                    val u = async { repositorio.usuarios() }
                    val s = async { repositorio.solicitudes() }
                    val f = async { repositorio.fichas() }
                    val i = async { repositorio.insumosBajos() }
                    val se = async { sensores.listar() }

                    val usuarios = u.await()
                    val solicitudes = s.await()
                    estado = estado.copy(
                        cargando = false,
                        error = (usuarios as? Resultado.Falla)?.mensaje.takeIf { solicitudes is Resultado.Falla },
                        usuarios = (usuarios as? Resultado.Ok)?.dato ?: estado.usuarios,
                        solicitudes = (solicitudes as? Resultado.Ok)?.dato ?: estado.solicitudes,
                        fichas = f.await() ?: estado.fichas,
                        insumos = i.await() ?: estado.insumos,
                        sensores = (se.await() as? Resultado.Ok)?.dato ?: estado.sensores,
                    )
                }
            } finally {
                cargandoAhora = false
            }
        }
    }

    fun aprobar(u: UsuarioAdmin) {
        estado = estado.copy(procesandoId = u.id, aviso = null, errorAccion = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.aprobar(u.id)) {
                is Resultado.Ok -> estado.copy(
                    procesandoId = null,
                    solicitudes = estado.solicitudes.filterNot { it.id == u.id },
                    aviso = "${u.nombreCompleto} ya puede entrar a AgroSoft.",
                )
                is Resultado.Falla -> estado.copy(procesandoId = null, errorAccion = r.mensaje)
            }
        }
    }

    fun pedirRechazo(u: UsuarioAdmin) {
        estado = estado.copy(rechazando = u, motivo = "", errorAccion = null, aviso = null)
    }

    fun cambiarMotivo(v: String) {
        estado = estado.copy(motivo = v.take(300))
    }

    fun cancelarRechazo() {
        estado = estado.copy(rechazando = null, motivo = "")
    }

    fun confirmarRechazo() {
        val u = estado.rechazando ?: return
        val motivo = estado.motivo.trim()
        if (motivo.length < 3) return
        estado = estado.copy(procesandoId = u.id, rechazando = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.rechazar(u.id, motivo)) {
                is Resultado.Ok -> estado.copy(
                    procesandoId = null,
                    motivo = "",
                    solicitudes = estado.solicitudes.filterNot { it.id == u.id },
                    aviso = "Se rechazó la solicitud de ${u.nombreCompleto}.",
                )
                is Resultado.Falla -> estado.copy(procesandoId = null, errorAccion = r.mensaje)
            }
        }
    }
}
