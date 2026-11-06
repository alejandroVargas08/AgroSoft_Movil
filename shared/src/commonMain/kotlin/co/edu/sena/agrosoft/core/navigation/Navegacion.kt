package co.edu.sena.agrosoft.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Rol (val etiqueta: String){
    APRENDIZ("Aprendiz"),
    INSTRUCTOR("Instructor"),
    ADMINISTRADOR("Administrador"),
}

enum class Pantalla(val principal: Boolean){
    INICIO(true),
    TRABAJO(true),
    AGROBOT(true),
    TERRENO(true),
    ALERTAS(true),
    MAS(true),
    SENSORES(false),
    DETALLE_SENSOR(false),
    DETALLE_VUELO(false),
    DETALLE_ESTUDIO(false),
    ACTIVIDADES(false),
    DETALLE_ACTIVIDAD(false),
    NOTIFICACIONES(false),
    PERFIL(false),
    SOLO_WEB(false),
}
fun tituloDe(pantalla: Pantalla, rol:Rol, nombre: String): String =when (pantalla){
    Pantalla.INICIO -> "Hola, $nombre"
    Pantalla.TRABAJO -> when (rol){
        Rol.APRENDIZ -> "Mis tareas"
        Rol.INSTRUCTOR -> "Por revisar"
        Rol.ADMINISTRADOR ->"Solicitudes"
    }
    Pantalla.AGROBOT -> "AgroBot"
    Pantalla.TERRENO -> "Terreno"
    Pantalla.ALERTAS -> "Alertas"
    Pantalla.MAS -> "Más"
    Pantalla.SENSORES -> "Sensores IoT"
    Pantalla.DETALLE_SENSOR -> "Estación Lote 3"
    Pantalla.DETALLE_VUELO -> "Vuelo 12"
    Pantalla.DETALLE_ESTUDIO-> "Resultado del estudio"
    Pantalla.ACTIVIDADES -> "Actividades"
    Pantalla.DETALLE_ACTIVIDAD -> "Detalle de actividad"
    Pantalla.NOTIFICACIONES -> "Notificaciones"
    Pantalla.PERFIL -> "Perfil"
    Pantalla.SOLO_WEB -> "AgroSoft web"
}
fun subtituloDe(pantalla: Pantalla, rol: Rol): String?= when (pantalla){
    Pantalla.INICIO -> rol.etiqueta
    Pantalla.AGROBOT -> "Tu asistente Agrícola con IA"
    Pantalla.DETALLE_SENSOR -> "Conectada · Hace 2 min"
    else -> null
}
class Navegador{
    var actual by mutableStateOf(Pantalla.INICIO)
        private set
    private val historial = mutableStateListOf<Pantalla>()

    fun ir(destino: Pantalla){
        if(destino.principal){
            cambiarPestana(destino)
        }else{
            historial.add(actual)
            actual = destino
        }
    }
    fun cambiarPestana(destino: Pantalla){
        historial.clear()
        actual = destino
    }
    fun volver(): Boolean{
        if(historial.isNotEmpty()){
            actual = historial.removeAt(historial.lastIndex)
            return true
        }
        if (actual != Pantalla.INICIO){
            actual = Pantalla.INICIO
            return true
        }
        return false
    }
}
