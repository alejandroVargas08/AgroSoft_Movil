package co.edu.sena.agrosoft.features.auth.presentation

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_agrosoft
import agrosoft.shared.generated.resources.logo_sena
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import org.jetbrains.compose.resources.painterResource

private val Esquinas = RoundedCornerShape(12.dp)

@Composable
fun LoginScreen(
    correo: String,
    password: String,
    mensajeError: String?,
    avisoCuenta: String?,
    cargando: Boolean,
    onCorreoChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onIniciarSesion: () -> Unit,
    onBiometria: () -> Unit,
    onOlvideContrasena: () -> Unit,
    onRegistro: () -> Unit,
) {
    var verPassword by remember { mutableStateOf(false) }
    val puedeEnviar = !cargando && correo.isNotBlank() && password.length >= 6

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Aqui se centra
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Encabezado
            Image(
                painter = painterResource(Res.drawable.logo_agrosoft),
                contentDescription = "AgroSoft",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(80.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
            )
            Spacer(Modifier.height(12.dp))
            Text("AgroSoft", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
            Spacer(Modifier.height(4.dp))
            Text("Trabajo de campo SENA", fontSize = 14.sp, color = AgroColors.TextSecondary)
            Spacer(Modifier.height(24.dp))

            // Tarjeta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x0F1A4D2E),
                        spotColor = Color(0x0F1A4D2E),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(AgroColors.Surface)
                    .border(1.dp, AgroColors.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Etiqueta("Correo")
                Spacer(Modifier.height(8.dp))
                Campo(
                    valor = correo,
                    onCambio = onCorreoChange,
                    marcador = "correo@sena.edu.co",
                    opciones = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )

                Spacer(Modifier.height(16.dp))
                Etiqueta("Contraseña")
                Spacer(Modifier.height(8.dp))
                Campo(
                    valor = password,
                    onCambio = onPasswordChange,
                    marcador = "Ingresa tu contraseña",
                    opciones = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    acciones = KeyboardActions(onDone = { if (puedeEnviar) onIniciarSesion() }),
                    transformacion = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    alFinal = {
                        Text(
                            if (verPassword) "Ocultar" else "Ver",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AgroColors.Primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { verPassword = !verPassword }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    },
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "¿Olvidaste tu contraseña?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AgroColors.Primary,
                    modifier = Modifier.clickable(onClick = onOlvideContrasena),
                )

                if (avisoCuenta != null) {
                    Spacer(Modifier.height(12.dp))
                    Mensaje(avisoCuenta, AgroColors.Warning, AgroColors.WarningSoft)
                }
                if (mensajeError != null) {
                    Spacer(Modifier.height(12.dp))
                    Mensaje(mensajeError, AgroColors.Error, AgroColors.ErrorSoft)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onIniciarSesion,
                    enabled = puedeEnviar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = Esquinas,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AgroColors.Primary,
                        contentColor = Color.White,
                        disabledContainerColor = AgroColors.Primary.copy(alpha = 0.5f),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (cargando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Iniciar sesión", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBiometria,
                    enabled = !cargando,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = Esquinas,
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, AgroColors.Primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = AgroColors.Primary,
                    ),
                ) {
                    Text("Entrar con biometría", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("¿No tienes cuenta? ", fontSize = 12.sp, color = AgroColors.TextSecondary)
                    Text(
                        "Regístrate",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgroColors.Primary,
                        modifier = Modifier.clickable(onClick = onRegistro),
                    )
                }
            }

            // Aquí esta lo del sena
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.logo_sena),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "SENA COLOMBIA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AgroColors.PrimaryDark,
                )
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
}

@Composable
private fun Mensaje(texto: String, color: Color, fondo: Color) {
    Text(
        texto,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clip(Esquinas)
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** Para enfoncar  */
@Composable
private fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    marcador: String,
    opciones: KeyboardOptions,
    acciones: KeyboardActions = KeyboardActions.Default,
    transformacion: VisualTransformation = VisualTransformation.None,
    alFinal: (@Composable () -> Unit)? = null,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()

    BasicTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = true,
        keyboardOptions = opciones,
        keyboardActions = acciones,
        visualTransformation = transformacion,
        interactionSource = interaccion,
        cursorBrush = SolidColor(AgroColors.Primary),
        textStyle = TextStyle(fontSize = 14.sp, color = AgroColors.Text),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { campoInterno ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(Esquinas)
                    .background(Color.White)
                    .border(1.dp, if (enfocado) AgroColors.Primary else AgroColors.Border, Esquinas)
                    .padding(start = 12.dp, end = if (alFinal != null) 4.dp else 12.dp),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (valor.isEmpty()) {
                        Text(marcador, fontSize = 14.sp, color = AgroColors.Placeholder)
                    }
                    campoInterno()
                }
                alFinal?.invoke()
            }
        },
    )
}
