package co.edu.sena.agrosoft.features.auth.presentation

import agrosoft.shared.generated.resources.Res
import agrosoft.shared.generated.resources.logo_agrosoft
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MailCheck
import org.jetbrains.compose.resources.painterResource

@Composable
fun CodigoDosPasosScreen(
    correo: String,
    codigo: String,
    mensaje: String?,
    error: String?,
    cargando: Boolean,
    onCodigoChange: (String) -> Unit,
    onVerificar: () -> Unit,
    onReenviar: () -> Unit,
    onVolver: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
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
            Image(
                painter = painterResource(Res.drawable.logo_agrosoft),
                contentDescription = "AgroSoft",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x0F1A4D2E), spotColor = Color(0x0F1A4D2E))
                    .clip(RoundedCornerShape(16.dp))
                    .background(AgroColors.Surface)
                    .border(1.dp, AgroColors.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(AgroColors.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Lucide.MailCheck, contentDescription = null, tint = AgroColors.Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Verifica tu identidad", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AgroColors.Title)
                Spacer(Modifier.height(6.dp))
                Text(
                    mensaje ?: "Te enviamos un código a tu correo.",
                    fontSize = 12.sp,
                    color = AgroColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(correo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Title)

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = codigo,
                    onValueChange = onCodigoChange,
                    singleLine = true,
                    enabled = !cargando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center,
                        color = AgroColors.Title,
                    ),
                    placeholder = {
                        Text(
                            "000000",
                            fontSize = 24.sp,
                            letterSpacing = 8.sp,
                            color = AgroColors.Placeholder,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onVerificar() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgroColors.Primary,
                        unfocusedBorderColor = AgroColors.Border,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        cursorColor = AgroColors.Primary,
                    ),
                )

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = AgroColors.Error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AgroColors.ErrorSoft)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AgroColors.Primary.copy(alpha = if (codigo.length == 6 && !cargando) 1f else 0.5f))
                        .clickable(enabled = codigo.length == 6 && !cargando, onClick = onVerificar),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cargando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Verificar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("¿No te llegó? ", fontSize = 12.sp, color = AgroColors.TextSecondary)
                    Text(
                        "Reenviar código",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgroColors.Primary,
                        modifier = Modifier.clickable(enabled = !cargando, onClick = onReenviar),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Volver al inicio de sesión",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AgroColors.TextMuted,
                    modifier = Modifier.clickable(enabled = !cargando, onClick = onVolver),
                )
            }
        }
    }
}
