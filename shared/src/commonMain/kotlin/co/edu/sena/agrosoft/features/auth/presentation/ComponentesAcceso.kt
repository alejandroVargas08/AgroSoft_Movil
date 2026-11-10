package co.edu.sena.agrosoft.features.auth.presentation

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_agrosoft
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MarcoAcceso(
    titulo: String,
    subtitulo: String,
    onVolver: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(AgroColors.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, AgroColors.Border, CircleShape)
                    .clickable(onClick = onVolver),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.ArrowLeft, contentDescription = "Volver", tint = AgroColors.Title, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(20.dp))
            Image(
                painterResource(Res.drawable.logo_agrosoft),
                contentDescription = "AgroSoft",
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(16.dp))
            Text(titulo, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
            Spacer(Modifier.height(4.dp))
            Text(subtitulo, fontSize = 14.sp, color = AgroColors.TextSecondary, lineHeight = 20.sp)
            Spacer(Modifier.height(24.dp))
            contenido()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun CampoAcceso(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String = "",
    teclado: KeyboardType = KeyboardType.Text,
    contrasena: Boolean = false,
    lineas: Int = 1,
    tamano: TextUnit = 15.sp,
    espaciado: TextUnit = TextUnit.Unspecified,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(etiqueta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, AgroColors.Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = if (lineas > 1) Alignment.Top else Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (valor.isEmpty()) {
                    Text(placeholder, fontSize = tamano, color = AgroColors.Placeholder, letterSpacing = espaciado)
                }
                BasicTextField(
                    value = valor,
                    onValueChange = onCambio,
                    singleLine = lineas == 1,
                    minLines = lineas,
                    textStyle = TextStyle(fontSize = tamano, color = AgroColors.Text, letterSpacing = espaciado),
                    cursorBrush = SolidColor(AgroColors.Primary),
                    keyboardOptions = KeyboardOptions(keyboardType = if (contrasena) KeyboardType.Password else teclado),
                    visualTransformation = if (contrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (contrasena) {
                Spacer(Modifier.width(8.dp))
                Text(
                    if (visible) "Ocultar" else "Ver",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AgroColors.Primary,
                    modifier = Modifier.clickable { visible = !visible },
                )
            }
        }
    }
}

@Composable
internal fun BotonAcceso(texto: String, cargando: Boolean, onClick: () -> Unit, habilitado: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Primary),
    ) {
        if (cargando) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun AvisoAcceso(texto: String, error: Boolean) {
    Text(
        texto,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = if (error) AgroColors.Error else AgroColors.Primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (error) AgroColors.ErrorSoft else AgroColors.SuccessSoft)
            .padding(12.dp),
    )
}

@Composable
internal fun EnlaceAcceso(texto: String, onClick: () -> Unit) {
    Text(
        texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AgroColors.Primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

internal fun correoValido(correo: String): Boolean {
    val c = correo.trim()
    val arroba = c.indexOf('@')
    return arroba > 0 && c.indexOf('.', arroba) > arroba + 1 && !c.endsWith(".") && ' ' !in c
}
