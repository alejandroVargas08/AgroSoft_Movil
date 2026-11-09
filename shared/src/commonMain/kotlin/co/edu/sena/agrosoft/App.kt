package co.edu.sena.agrosoft

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_agrosoft
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import co.edu.sena.agrosoft.features.actividades.presentation.ActividadesScreen
import co.edu.sena.agrosoft.features.actividades.presentation.ActividadesViewModel
import co.edu.sena.agrosoft.features.actividades.presentation.DetalleActividadScreen
import co.edu.sena.agrosoft.features.actividades.presentation.NuevaActividadScreen
import co.edu.sena.agrosoft.features.admin.presentation.AdminViewModel
import co.edu.sena.agrosoft.features.admin.presentation.AlertasScreen
import co.edu.sena.agrosoft.features.admin.presentation.InicioAdminScreen
import co.edu.sena.agrosoft.features.admin.presentation.SolicitudesScreen
import co.edu.sena.agrosoft.features.agrobot.presentation.AgroBotScreen
import co.edu.sena.agrosoft.features.agrobot.presentation.AgroBotViewModel
import co.edu.sena.agrosoft.features.auth.data.Sesion
import co.edu.sena.agrosoft.features.auth.presentation.CodigoDosPasosScreen
import co.edu.sena.agrosoft.features.auth.presentation.LoginScreen
import co.edu.sena.agrosoft.features.auth.presentation.LoginViewModel
import co.edu.sena.agrosoft.features.inicio.presentation.InicioAprendizScreen
import co.edu.sena.agrosoft.features.inicio.presentation.InicioInstructorScreen
import co.edu.sena.agrosoft.features.mas.presentation.MasScreen
import co.edu.sena.agrosoft.features.notificaciones.presentation.NotificacionesScreen
import co.edu.sena.agrosoft.features.notificaciones.presentation.NotificacionesViewModel
import co.edu.sena.agrosoft.features.perfil.presentation.PerfilScreen
import co.edu.sena.agrosoft.features.revision.presentation.RevisionScreen
import co.edu.sena.agrosoft.features.sensores.presentation.DetalleSensorScreen
import co.edu.sena.agrosoft.features.sensores.presentation.FiltroSensores
import co.edu.sena.agrosoft.features.sensores.presentation.SensoresScreen
import co.edu.sena.agrosoft.features.sensores.presentation.SensoresViewModel
import co.edu.sena.agrosoft.features.tareas.presentation.TareasAprendizScreen
import co.edu.sena.agrosoft.features.terreno.presentation.DetalleVueloScreen
import co.edu.sena.agrosoft.features.terreno.presentation.TerrenoScreen
import co.edu.sena.agrosoft.features.terreno.presentation.TerrenoViewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun App() {
    setSingletonImageLoaderFactory { contexto ->
        ImageLoader.Builder(contexto)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    AgroSoftTheme {
        val login = viewModel { LoginViewModel() }
        val estado = login.estado
        val sesion = estado.sesion
        val correoCodigo = estado.correoCodigo

        when {
            estado.verificando -> PantallaCarga()
            sesion != null -> AppPrincipal(
                sesion = sesion,
                onSesionActualizada = login::actualizarSesion,
                onCerrarSesion = login::cerrarSesion,
            )
            correoCodigo != null -> CodigoDosPasosScreen(
                correo = correoCodigo,
                codigo = estado.codigo,
                mensaje = estado.mensajeCodigo,
                error = estado.errorCodigo,
                cargando = estado.cargando,
                onCodigoChange = login::cambiarCodigo,
                onVerificar = login::verificarCodigo,
                onReenviar = login::reenviarCodigo,
                onVolver = login::cancelarCodigo,
            )
            else -> LoginScreen(
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
private fun AppPrincipal(
    sesion: Sesion,
    onSesionActualizada: (Sesion) -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val navegador = remember(sesion.id) { Navegador() }
    val pantalla = navegador.actual
    val esAdmin = sesion.rol == Rol.ADMINISTRADOR
    val avisos = viewModel(key = "avisos-${sesion.id}") { NotificacionesViewModel(sesion.id, sesion.rol) }
    val sensores = viewModel(key = "sensores-${sesion.id}") { SensoresViewModel() }
    val terreno = viewModel(key = "terreno-${sesion.id}") { TerrenoViewModel() }
    val actividades = viewModel(key = "actividades-${sesion.id}") { ActividadesViewModel() }
    val admin = viewModel(key = "admin-${sesion.id}") { AdminViewModel() }
    val agrobot = viewModel(key = "agrobot-${sesion.id}") { AgroBotViewModel(sesion.id) }
    val creandoTarea = pantalla == Pantalla.ACTIVIDADES && actividades.estado.creando
    val chatAbierto = pantalla == Pantalla.AGROBOT && agrobot.estado.abierta

    LaunchedEffect(pantalla) {
        avisos.refrescar()
        if (esAdmin) admin.cargar()
    }
    LaunchedEffect(sesion.id) {
        while (true) {
            delay(60_000)
            avisos.refrescar()
        }
    }

    val titulo = when {
        creandoTarea -> "Nueva tarea"
        chatAbierto -> agrobot.estado.titulo?.takeIf { it.isNotBlank() } ?: "AgroBot"
        pantalla == Pantalla.DETALLE_SENSOR -> sensores.estado.seleccionado?.nombreSensor
        pantalla == Pantalla.DETALLE_VUELO -> terreno.estado.seleccionado?.nombre
        pantalla == Pantalla.DETALLE_ACTIVIDAD -> actividades.estado.seleccionada?.actividad?.titulo
        else -> null
    } ?: tituloDe(pantalla, sesion.rol, sesion.nombre)

    val subtitulo = when {
        creandoTarea -> "Crea y asigna en un paso"
        chatAbierto -> "Asistente agrícola de AgroSoft"
        else -> subtituloDe(pantalla, sesion.rol)
    }

    val abrirSensor: (Int) -> Unit = { id ->
        sensores.cargar()
        sensores.abrir(id)
        navegador.ir(Pantalla.DETALLE_SENSOR)
    }

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
                titulo = titulo,
                subtitulo = subtitulo,
                esInicio = pantalla == Pantalla.INICIO,
                iniciales = sesion.iniciales,
                notificaciones = avisos.estado.noLeidas,
                onVolver = when {
                    creandoTarea -> ({ actividades.cancelarCreacion() })
                    chatAbierto -> ({ agrobot.cerrarChat() })
                    pantalla.principal -> null
                    else -> ({ navegador.volver() })
                },
                onAcciones = {
                    when (sesion.rol) {
                        Rol.INSTRUCTOR -> {
                            actividades.nuevaTarea()
                            if (pantalla != Pantalla.ACTIVIDADES) navegador.ir(Pantalla.ACTIVIDADES)
                        }
                        Rol.ADMINISTRADOR -> navegador.cambiarPestana(Pantalla.TRABAJO)
                        Rol.APRENDIZ -> navegador.cambiarPestana(Pantalla.TRABAJO)
                    }
                },
                onCampana = { navegador.ir(Pantalla.NOTIFICACIONES) },
                onAvatar = { navegador.ir(Pantalla.PERFIL) },
            )

            if (pantalla == Pantalla.AGROBOT) {
                AgroBotScreen(
                    vm = agrobot,
                    nombre = sesion.nombre,
                    rol = sesion.rol,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = if (pantalla.principal) 110.dp else 24.dp),
                ) {
                    Contenido(
                        pantalla = pantalla,
                        sesion = sesion,
                        navegador = navegador,
                        avisos = avisos,
                        sensores = sensores,
                        terreno = terreno,
                        actividades = actividades,
                        admin = admin,
                        abrirSensor = abrirSensor,
                        onSesionActualizada = onSesionActualizada,
                        onCerrarSesion = onCerrarSesion,
                    )
                }
            }
        }

        if (pantalla.principal && !chatAbierto) {
            BarraInferior(
                rol = sesion.rol,
                actual = pantalla,
                insignias = buildMap {
                    put(Pantalla.TRABAJO, avisos.estado.pendientesTrabajo)
                    if (esAdmin) put(Pantalla.ALERTAS, admin.estado.totalAlertas)
                },
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
private fun Contenido(
    pantalla: Pantalla,
    sesion: Sesion,
    navegador: Navegador,
    avisos: NotificacionesViewModel,
    sensores: SensoresViewModel,
    terreno: TerrenoViewModel,
    actividades: ActividadesViewModel,
    admin: AdminViewModel,
    abrirSensor: (Int) -> Unit,
    onSesionActualizada: (Sesion) -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val esAdmin = sesion.rol == Rol.ADMINISTRADOR
    when (pantalla) {
        Pantalla.INICIO -> when (sesion.rol) {
            Rol.APRENDIZ -> InicioAprendizScreen(
                sesionId = sesion.id,
                onAbrirTareas = { navegador.cambiarPestana(Pantalla.TRABAJO) },
                onAbrirSensores = {
                    sensores.cambiarFiltro(FiltroSensores.TODOS)
                    navegador.ir(Pantalla.SENSORES)
                },
                onAbrirVuelo = {
                    terreno.limpiarSeleccion()
                    navegador.ir(Pantalla.DETALLE_VUELO)
                },
            )
            Rol.INSTRUCTOR -> InicioInstructorScreen(
                sesionId = sesion.id,
                onAbrirRevisar = { navegador.cambiarPestana(Pantalla.TRABAJO) },
                onAbrirSensores = {
                    sensores.cambiarFiltro(FiltroSensores.ALERTA)
                    navegador.ir(Pantalla.SENSORES)
                },
            )
            Rol.ADMINISTRADOR -> InicioAdminScreen(
                vm = admin,
                onSolicitudes = { navegador.cambiarPestana(Pantalla.TRABAJO) },
                onAlertas = { navegador.cambiarPestana(Pantalla.ALERTAS) },
                onSensores = {
                    sensores.cambiarFiltro(FiltroSensores.TODOS)
                    navegador.ir(Pantalla.SENSORES)
                },
            )
        }
        Pantalla.TRABAJO -> when (sesion.rol) {
            Rol.APRENDIZ -> TareasAprendizScreen(sesionId = sesion.id)
            Rol.INSTRUCTOR -> RevisionScreen(sesionId = sesion.id)
            Rol.ADMINISTRADOR -> SolicitudesScreen(vm = admin)
        }
        Pantalla.ALERTAS -> AlertasScreen(
            vm = admin,
            onAbrirSensor = abrirSensor,
            onAbrirInsumo = { navegador.ir(Pantalla.SOLO_WEB) },
        )
        Pantalla.ACTIVIDADES -> if (actividades.estado.creando) {
            NuevaActividadScreen(vm = actividades)
        } else {
            ActividadesScreen(
                vm = actividades,
                onAbrir = { id ->
                    actividades.abrir(id)
                    navegador.ir(Pantalla.DETALLE_ACTIVIDAD)
                },
            )
        }
        Pantalla.DETALLE_ACTIVIDAD -> DetalleActividadScreen(
            vm = actividades,
            onRevisar = { navegador.cambiarPestana(Pantalla.TRABAJO) },
        )
        Pantalla.TERRENO -> TerrenoScreen(
            vm = terreno,
            onAbrirVuelo = { id ->
                terreno.abrir(id)
                navegador.ir(Pantalla.DETALLE_VUELO)
            },
        )
        Pantalla.DETALLE_VUELO -> DetalleVueloScreen(
            vm = terreno,
            puedeAnalizar = sesion.rol != Rol.APRENDIZ,
        )
        Pantalla.SENSORES -> SensoresScreen(
            vm = sensores,
            onAbrir = { id ->
                sensores.abrir(id)
                navegador.ir(Pantalla.DETALLE_SENSOR)
            },
        )
        Pantalla.DETALLE_SENSOR -> DetalleSensorScreen(vm = sensores)
        Pantalla.NOTIFICACIONES -> NotificacionesScreen(
            vm = avisos,
            onAbrir = { n ->
                when (n.tipo) {
                    "tarea", "solicitud" -> navegador.cambiarPestana(Pantalla.TRABAJO)
                    "insumo_bajo", "herramienta_danada" -> if (esAdmin) navegador.cambiarPestana(Pantalla.ALERTAS)
                }
            },
        )
        Pantalla.PERFIL -> PerfilScreen(
            sesion = sesion,
            onActualizado = { p ->
                val completo = p.nombreCompleto
                onSesionActualizada(
                    sesion.copy(
                        nombre = p.nombre.trim().substringBefore(" "),
                        nombreCompleto = completo,
                        iniciales = completo.split(" ").filter { it.isNotBlank() }.take(2)
                            .map { it.first().uppercaseChar() }.joinToString(""),
                        correo = p.correo ?: sesion.correo,
                    )
                )
            },
            onNotificaciones = { navegador.ir(Pantalla.NOTIFICACIONES) },
            onCerrarSesion = onCerrarSesion,
        )
        Pantalla.MAS -> MasScreen(
            sesion = sesion,
            onIr = { destino ->
                if (destino == Pantalla.SENSORES) sensores.cambiarFiltro(FiltroSensores.TODOS)
                if (destino == Pantalla.ACTIVIDADES) actividades.cancelarCreacion()
                if (destino.principal) navegador.cambiarPestana(destino) else navegador.ir(destino)
            },
            onCerrarSesion = onCerrarSesion,
        )
        Pantalla.SOLO_WEB -> SoloWeb()
        else -> EnConstruccion(tituloDe(pantalla, sesion.rol, sesion.nombre))
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
private fun SoloWeb() {
    Tarjeta(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Lucide.Monitor, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text("Disponible en la web", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Spacer(Modifier.height(6.dp))
            Text(
                "Esta función se usa desde la versión web de AgroSoft. Muy pronto también estará en la app.",
                fontSize = 12.sp,
                color = AgroColors.TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
            )
        }
    }
}
