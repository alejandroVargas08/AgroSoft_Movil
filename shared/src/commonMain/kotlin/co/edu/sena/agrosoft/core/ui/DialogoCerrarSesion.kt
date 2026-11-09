package co.edu.sena.agrosoft.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.sena.agrosoft.core.theme.AgroColors

@Composable
fun DialogoCerrarSesion(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = { Text("¿Cerrar sesión?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AgroColors.Text) },
        text = { Text("Tendrás que volver a identificarte para entrar a AgroSoft.", fontSize = 14.sp, color = AgroColors.TextSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgroColors.Error, contentColor = Color.White),
            ) { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar, shape = RoundedCornerShape(12.dp)) {
                Text("Cancelar", color = AgroColors.Text, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
