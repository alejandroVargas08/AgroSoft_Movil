package co.edu.sena.agrosoft

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_agrosoft
import agrosoft.shared.generated.resources.logo_sena
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import co.edu.sena.agrosoft.core.seguridad.AccesoBiometrico
import co.edu.sena.agrosoft.core.seguridad.Credenciales
import co.edu.sena.agrosoft.core.seguridad.autenticarBiometria
import co.edu.sena.agrosoft.core.theme.AgroColors
import co.edu.sena.agrosoft.core.theme.AgroSoftTheme
import co.edu.sena.agrosoft.core.ui.BarraInferiorCristal
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
import co.edu.sena.agrosoft.features.auth.presentation.RecuperarScreen
import co.edu.sena.agrosoft.features.auth.presentation.RegistroScreen
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
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private enum class Acceso { LOGIN, RECUPERAR, REGISTRO }

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
        var acceso by remember { mutableStateOf(Acceso.LOGIN) }

        var bienvenidaLista by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(1_200)
            bienvenidaLista = true
        }

        val alcance = rememberCoroutineScope()
        val tipoBio = AccesoBiometrico.tipo
        var pendientes by remember { mutableStateOf<Credenciales?>(null) }
        var porBiometria by remember { mutableStateOf(false) }
        var autoIntentado by remember { mutableStateOf(false) }
        var aviso by remember { mutableStateOf<String?>(null) }

        val ingresarConBiometria: () -> Unit = {
            when {
                tipoBio == null ->
                    aviso = "Este dispositivo no tiene Face ID ni huella configurados. Actívalos en los ajustes del teléfono."
                !AccesoBiometrico.activo -> {
                    AccesoBiometrico.rechazado = false
                    aviso = "Primero ingresa con tu correo y contraseña. Después te preguntaremos si quieres activar ${tipoBio.nombre}."
                }
                else -> alcance.launch {
                    val c = AccesoBiometrico.obtener("Ingresa a AgroSoft")
                    if (c != null) {
                        login.cambiarCorreo(c.correo)
                        login.cambiarPassword(c.password)
                        porBiometria = true
                        login.iniciarSesion()
                    }
                }
            }
        }

        LaunchedEffect(estado.error) {
            if (porBiometria && estado.error != null) {
                porBiometria = false
                AccesoBiometrico.desactivar()
                aviso = "No se pudo ingresar con ${tipoBio?.nombre ?: "biometría"}. Si cambiaste tu contraseña, ingresa con ella y vuelve a activarlo."
            }
        }

        LaunchedEffect(sesion?.id) {
            if (sesion != null) {
                porBiometria = false
                val puedeOfrecer = AccesoBiometrico.tipo != null && !AccesoBiometrico.activo && !AccesoBiometrico.rechazado
                if (!puedeOfrecer) pendientes = null
            }
        }

        val volverAlLogin: (String) -> Unit = { correo ->
            if (correo.isNotBlank()) login.cambiarCorreo(correo)
            acceso = Acceso.LOGIN
        }

        when {
            estado.verificando || !bienvenidaLista -> PantallaCarga()
            sesion != null -> AppPrincipal(
                sesion = sesion,
                onSesionActualizada = login::actualizarSesion,
                onCerrarSesion = {
                    acceso = Acceso.LOGIN
                    login.cerrarSesion()
                },
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
            acceso == Acceso.RECUPERAR -> RecuperarScreen(
                correoInicial = estado.correo,
                onVolver = { acceso = Acceso.LOGIN },
                onTerminar = volverAlLogin,
            )
            acceso == Acceso.REGISTRO -> RegistroScreen(
                onVolver = { acceso = Acceso.LOGIN },
                onTerminar = volverAlLogin,
            )
            else -> {
                LaunchedEffect(Unit) {
                    if (!autoIntentado && AccesoBiometrico.tipo != null && AccesoBiometrico.activo) {
                        autoIntentado = true
                        ingresarConBiometria()
                    }
                }
                LoginScreen(
                    correo = estado.correo,
                    password = estado.password,
                    mensajeError = estado.error,
                    avisoCuenta = estado.aviso,
                    cargando = estado.cargando,
                    onCorreoChange = login::cambiarCorreo,
                    onPasswordChange = login::cambiarPassword,
                    onIniciarSesion = {
                        pendientes = Credenciales(estado.correo.trim(), estado.password)
                        porBiometria = false
                        login.iniciarSesion()
                    },
                    onBiometria = ingresarConBiometria,
                    onOlvideContrasena = { acceso = Acceso.RECUPERAR },
                    onRegistro = { acceso = Acceso.REGISTRO },
                )
            }
        }

        val credenciales = pendientes
        if (sesion != null && credenciales != null && tipoBio != null) {
            AlertDialog(
                onDismissRequest = { pendientes = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = { Text("¿Ingresar con ${tipoBio.nombre}?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text) },
                text = {
                    Text(
                        "La próxima vez podrás entrar a AgroSoft con ${tipoBio.nombre}, sin escribir tu contraseña. Tus datos quedan cifrados en este dispositivo.",
                        fontSize = 14.sp,
                        color = AgroColors.TextSecondary,
                        lineHeight = 20.sp,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            alcance.launch {
                                if (autenticarBiometria("Confirma para activar ${tipoBio.nombre}")) {
                                    AccesoBiometrico.activar(credenciales)
                                }
                                pendientes = null
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
                    ) { Text("Activar", fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        AccesoBiometrico.rechazado = true
                        pendientes = null
                    }) { Text("Ahora no", color = AgroColors.TextSecondary) }
                },
            )
        }

        aviso?.let { texto ->
            AlertDialog(
                onDismissRequest = { aviso = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = { Text(tipoBio?.nombre ?: "Biometría", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text) },
                text = { Text(texto, fontSize = 14.sp, color = AgroColors.TextSecondary, lineHeight = 20.sp) },
                confirmButton = {
                    Button(
                        onClick = { aviso = null },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
                    ) { Text("Entendido", fontWeight = FontWeight.SemiBold) }
                },
            )
        }
    }
}

@Composable
private fun PantallaCarga() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alfa by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "bienvenida",
    )

    Box(Modifier.fillMaxSize().background(AgroColors.Background)) {
        Image(
            painter = painterResource(Res.drawable.logo_agrosoft),
            contentDescription = "AgroSoft",
            modifier = Modifier.align(Alignment.Center).size(88.dp).clip(RoundedCornerShape(20.dp)),
        )
        Column(
            modifier = Modifier.align(Alignment.Center).offset(y = 86.dp).alpha(alfa),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AgroSoft", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Spacer(Modifier.height(2.dp))
            Text("Trabajo de campo SENA", fontSize = 13.sp, color = AgroColors.TextMuted)
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 32.dp)
                .alpha(alfa * 0.8f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo_sena),
                contentDescription = "SENA",
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("SENA COLOMBIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = AgroColors.TextMuted)
        }
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
    val hazeState = rememberHazeState()
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
                .background(AgroColors.Background)
                .hazeSource(hazeState)
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
            BarraInferiorCristal(
                rol = sesion.rol,
                actual = pantalla,
                insignias = buildMap {
                    put(Pantalla.TRABAJO, avisos.estado.pendientesTrabajo)
                    if (esAdmin) put(Pantalla.ALERTAS, admin.estado.totalAlertas)
                },
                hazeState = hazeState,
                onCambiar = { navegador.cambiarPestana(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp),
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
