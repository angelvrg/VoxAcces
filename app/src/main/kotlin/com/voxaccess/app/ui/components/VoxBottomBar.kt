package com.voxaccess.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxaccess.app.ui.theme.*

enum class VoxPantalla(val titulo: String, val icono: ImageVector) {
    ENTRADA("ENTRADA", Icons.Default.Mic),
    REPRODUCTOR("REPRODUCTOR", Icons.Default.PlayArrow),
    AJUSTES("AJUSTES", Icons.Default.Tune)
}

@Composable
fun VoxBottomBar(
    pantallaActual: VoxPantalla,
    alSeleccionarPantalla: (VoxPantalla) -> Unit,
    modificador: Modifier = Modifier
) {
    Row(
        modifier = modificador
            .fillMaxWidth()
            .background(FondoOscuro)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VoxPantalla.values().forEach { pantalla ->
            val seleccionada = pantalla == pantallaActual
            val colorFondo = if (seleccionada) NaranjaVox else VarianteSuperficieOscura
            val colorContenido = if (seleccionada) TextoOscuroSobreNaranja else TextoBlanco

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorFondo)
                    .clickable(
                        role = Role.Tab,
                        onClick = { alSeleccionarPantalla(pantalla) }
                    )
                    .semantics {
                        contentDescription = "Pestaña ${pantalla.titulo.lowercase()}"
                        selected = seleccionada
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = pantalla.icono,
                        contentDescription = null,
                        tint = colorContenido,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pantalla.titulo,
                        color = colorContenido,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
