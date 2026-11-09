package co.edu.sena.agrosoft.features.mas.presentation

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_sena
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.navigation.Pantalla
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.ui.DialogoCerrarSesion
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.features.auth.data.Sesion
import com.composables.icons.lucide.*
import org.jetbrains.compose.resources.painterResource

private data class OpcionMas(
    val titulo: String,
    val subtitulo: String,
    val icono: ImageVector,
    val destino: Pantalla,
)

@Composable
fun MasScreen(
    sesion: Sesion,
    onIr: (Pantalla) -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val vm = viewModel(key = "mas-${sesion.id}") { MasViewModel(sesion.rol) }
    val e = vm.estado
    val r = e.resumen

    LaunchedEffect(Unit) { vm.cargar() }

    val textoSensores = when {
        r?.sensores == null -> "Estaciones y lecturas"
        else -> buildString {
            append(if (r.sensores == 1) "1 estación" else "${r.sensores} estaciones")
            val alertas = r.sensoresEnAlerta ?: 0
            if (alertas > 0) append(" · ").append(if (alertas == 1) "1 alerta" else "$alertas alertas")
        }
    }
    val textoNovedades = r?.novedadesPendientes?.let {
        when (it) {
            0 -> "Sin pendientes"
            1 -> "1 pendiente"
            else -> "$it pendientes"
        }
    } ?: "Reportes de campo"

    val opciones = buildList {
        add(OpcionMas("Sensores IoT", textoSensores, Lucide.Radio, Pantalla.SENSORES))
        if (sesion.rol == Rol.ADMINISTRADOR) add(OpcionMas("Terreno", "Estudios y vuelos", Lucide.Map, Pantalla.TERRENO))
        if (sesion.rol == Rol.INSTRUCTOR) add(OpcionMas("Actividades", "Tareas que has asignado", Lucide.ClipboardCheck, Pantalla.ACTIVIDADES))
        when (sesion.rol) {
            Rol.APRENDIZ -> add(OpcionMas("Mis novedades", textoNovedades, Lucide.TriangleAlert, Pantalla.SOLO_WEB))
            Rol.INSTRUCTOR -> add(OpcionMas("Novedades", textoNovedades, Lucide.TriangleAlert, Pantalla.SOLO_WEB))
            Rol.ADMINISTRADOR -> add(OpcionMas("Usuarios", "Gestión rápida", Lucide.Users, Pantalla.SOLO_WEB))
        }
        add(OpcionMas("Wiki EPA", "Plagas y enfermedades", Lucide.Leaf, Pantalla.SOLO_WEB))
        add(OpcionMas("Perfil", sesion.nombreCompleto, Lucide.User, Pantalla.PERFIL))
        add(OpcionMas("Configuración", "Notificaciones y tema", Lucide.Settings, Pantalla.SOLO_WEB))
    }

    Column(Modifier.fillMaxWidth()) {
        TituloSeccion("Herramientas de campo")
        Tarjeta(modifier = Modifier.fillMaxWidth()) {
            opciones.forEachIndexed { i, op ->
                if (i > 0) HorizontalDivider(color = AgroColors.Border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Fila(op) { onIr(op.destino) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = vm::pedirSalida,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AgroColors.Error),
            border = BorderStroke(1.dp, AgroColors.Error),
        ) {
            Icon(Lucide.LogOut, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth().alpha(0.7f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.logo_sena), contentDescription = "SENA", modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
            Text("SENA COLOMBIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = AgroColors.TextMuted)
        }
    }

    if (e.confirmarSalida) {
        DialogoCerrarSesion(
            onConfirmar = {
                vm.cancelarSalida()
                onCerrarSesion()
            },
            onCancelar = vm::cancelarSalida,
        )
    }
}

@Composable
private fun Fila(op: OpcionMas, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(AgroColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(op.icono, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(op.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Text(op.subtitulo, fontSize = 11.sp, color = AgroColors.TextMuted, maxLines = 1)
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = AgroColors.TextMuted, modifier = Modifier.size(16.dp))
    }
}
