package com.voxaccess.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxaccess.app.R
import com.voxaccess.app.ui.theme.NaranjaVox
import com.voxaccess.app.ui.theme.TextoBlanco

// Encabezado principal con el logotipo de la aplicación
@Composable
fun VoxHeader(
    subtitulo: String,
    modificador: Modifier = Modifier
) {
    Row(
        modifier = modificador
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .semantics { heading() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logotipo de VoxAccess",
            modifier = Modifier
                .height(36.dp)
                .width(62.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "VOXACCESS",
                color = TextoBlanco,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = subtitulo,
                color = NaranjaVox,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
