package co.edu.sena.agrosoft.features.mas.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.features.mas.data.MasRepositorio
import co.edu.sena.agrosoft.features.mas.data.ResumenMas
import kotlinx.coroutines.launch

data class MasEstado(
    val resumen: ResumenMas? = null,
    val confirmarSalida: Boolean = false,
)

class MasViewModel(
    private val rol: Rol,
    private val repositorio: MasRepositorio = MasRepositorio(),
) : ViewModel() {

    var estado by mutableStateOf(MasEstado())
        private set

    fun cargar() {
        viewModelScope.launch {
            estado = estado.copy(resumen = repositorio.resumen(rol))
        }
    }

    fun pedirSalida() { estado = estado.copy(confirmarSalida = true) }
    fun cancelarSalida() { estado = estado.copy(confirmarSalida = false) }
}
