package co.edu.sena.agrosoft

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_agrosoft
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.sena.agrosoft.core.navigation.Navegador
import co.edu.sena.agrosoft.core.navigation.Pantalla
import co.edu.sena.agrosoft.core.navigation.Rol
import co.edu.sena.agrosoft.core.navigation.subtituloDe
import co.edu.sena.agrosoft.core.navigation.tituloDe
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.theme.AgroSoftTheme
import co.edu.sena.agrosoft.core.ui.BarraInferior
import co.edu.sena.agrosoft.core.ui.BarraSuperior
import co.edu.sena.agrosoft.core.ui.Tarjeta
import co.edu.sena.agrosoft.core.ui.TituloSeccion
import co.edu.sena.agrosoft.features.auth.data.Sesion
import co.edu.sena.agrosoft.features.auth.presentation.LoginScreen
import co.edu.sena.agrosoft.features.auth.presentation.LoginViewModel
import co.edu.sena.agrosoft.features.inicio.presentation.InicioAprendizScreen
import org.jetbrains.compose.resources.painterResource

@Composable
fun App() {
    AgroSoftTheme {
        val login = viewModel { LoginViewModel() }
        val estado = login.estado
        val sesion = estado.sesion

        when {
            estado.verificando -> PantallaCarga()
            sesion == null -> LoginScreen(
                correo = estado.correo,
                password = estado.password,
                mensajeError = estado.error,
                avisoCuenta = estado.aviso,
                cargando = estado.cargando,
                onCorreoChange = login::cambiarCorreo,
                onPasswordChange = login::cambiarPassword,
                onIniciarSesion = login::iniciarSesion,
                onBiometria = {},
                onOlvideContrasena = {},
                onRegistro = {},
            )
            else -> AppPrincipal(sesion = sesion, onCerrarSesion = login::cerrarSesion)
        }
    }
}

@Composable
private fun PantallaCarga() {
    Box(
        modifier = Modifier.fillMaxSize().background(AgroColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo_agrosoft),
            contentDescription = "AgroSoft",
            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)),
        )
    }
}

@Composable
private fun AppPrincipal(sesion: Sesion, onCerrarSesion: () -> Unit) {
    val navegador = remember(sesion.id) { Navegador() }
    val pantalla = navegador.actual

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            Spacer(Modifier.height(8.dp))
            BarraSuperior(
                titulo = tituloDe(pantalla, sesion.rol, sesion.nombre),
                subtitulo = subtituloDe(pantalla, sesion.rol),
                esInicio = pantalla == Pantalla.INICIO,
                iniciales = sesion.iniciales,
                notificaciones = 3,
                onVolver = if (pantalla.principal) null else ({ navegador.volver() }),
                onAcciones = {},
                onCampana = { navegador.ir(Pantalla.NOTIFICACIONES) },
                onAvatar = { navegador.ir(Pantalla.PERFIL) },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (pantalla.principal) 110.dp else 24.dp),
            ) {
                when (pantalla) {
                    Pantalla.INICIO -> if (sesion.rol == Rol.APRENDIZ) {
                        InicioAprendizScreen(
                            sesionId = sesion.id,
                            onAbrirTareas = { navegador.cambiarPestana(Pantalla.TRABAJO) },
                            onAbrirSensores = { navegador.ir(Pantalla.SENSORES) },
                            onAbrirVuelo = { navegador.ir(Pantalla.DETALLE_VUELO) },
                        )
                    } else {
                        EnConstruccion(tituloDe(pantalla, sesion.rol, sesion.nombre))
                    }
                    Pantalla.MAS -> PantallaMasTemporal(navegador, onCerrarSesion)
                    else -> EnConstruccion(tituloDe(pantalla, sesion.rol, sesion.nombre))
                }
            }
        }

        if (pantalla.principal) {
            BarraInferior(
                rol = sesion.rol,
                actual = pantalla,
                insignias = mapOf(
                    Pantalla.TRABAJO to if (sesion.rol == Rol.ADMINISTRADOR) 4 else 3,
                    Pantalla.ALERTAS to 4,
                ),
                onCambiar = { navegador.cambiarPestana(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun EnConstruccion(nombre: String) {
    Tarjeta(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Spacer(Modifier.height(6.dp))
            Text(
                "Esta pantalla está en construcción.",
                fontSize = 12.sp,
                color = AgroColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PantallaMasTemporal(navegador: Navegador, onCerrarSesion: () -> Unit) {
    Column {
        TituloSeccion("Probar pantallas internas")
        listOf(
            "Sensores IoT" to Pantalla.SENSORES,
            "Actividades" to Pantalla.ACTIVIDADES,
            "Perfil" to Pantalla.PERFIL,
        ).forEach { (texto, destino) ->
            Tarjeta(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                onClick = { navegador.ir(destino) },
            ) {
                Text(
                    texto,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroColors.Title,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AgroColors.Error),
            border = BorderStroke(1.dp, AgroColors.Error),
        ) {
            Text("Cerrar sesión", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
