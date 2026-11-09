package co.edu.sena.agrosoft.features.inicio.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.features.inicio.data.AprendizResumen
import co.edu.sena.agrosoft.features.inicio.data.InicioInstructorRepositorio
import co.edu.sena.agrosoft.features.tareas.data.Resultado
import kotlinx.coroutines.launch

data class InicioInstructorEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val aprendices: List<AprendizResumen> = emptyList(),
    val novedades: Int? = null,
    val sensoresEnAlerta: Int? = null,
    val ficha: String? = null,
    val verTodos: Boolean = false,
) {
    val fichas: List<String> get() = aprendices.mapNotNull { it.idFicha }.distinct().sorted()
    val visibles: List<AprendizResumen> get() = if (ficha == null) aprendices else aprendices.filter { it.idFicha == ficha }
    val porRevisar: Int get() = visibles.sumOf { it.entregadas }
    val vencidas: Int get() = visibles.sumOf { it.vencidas }
    val etiquetaFicha: String
        get() = when {
            ficha != null -> "Ficha $ficha"
            fichas.size == 1 -> "Ficha ${fichas.first()}"
            fichas.isEmpty() -> "Mis aprendices"
            else -> "Todas mis fichas"
        }
}

class InicioInstructorViewModel(
    private val repositorio: InicioInstructorRepositorio = InicioInstructorRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(InicioInstructorEstado())
        private set

    fun cargar() {
        estado = estado.copy(cargando = true, error = null)
        viewModelScope.launch {
            estado = when (val r = repositorio.cargar()) {
                is Resultado.Ok -> {
                    val lista = r.dato.aprendices.sortedWith(
                        compareByDescending<AprendizResumen> { it.entregadas }
                            .thenByDescending { it.vencidas }
                            .thenBy { it.apellido }
                    )
                    estado.copy(
                        cargando = false,
                        aprendices = lista,
                        novedades = r.dato.novedadesPendientes,
                        sensoresEnAlerta = r.dato.sensoresEnAlerta,
                        ficha = estado.ficha?.takeIf { f -> lista.any { it.idFicha == f } },
                    )
                }
                is Resultado.Falla -> estado.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun elegirFicha(ficha: String?) {
        estado = estado.copy(ficha = ficha, verTodos = false)
    }

    fun alternarTodos() {
        estado = estado.copy(verTodos = !estado.verTodos)
    }
}
