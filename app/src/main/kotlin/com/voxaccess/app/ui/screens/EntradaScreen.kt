package com.voxaccess.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxaccess.app.ui.components.VoxHeader
import com.voxaccess.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntradaScreen(
    cargando: Boolean,
    mensajeEstado: String,
    alProcesarUrl: (String) -> Unit,
    alIniciarDictado: () -> Unit,
    alVibrarToque: () -> Unit,
    modificador: Modifier = Modifier
) {
    val contexto = LocalContext.current
    var mostrarModalOpciones by remember { mutableStateOf(false) }
    var textoManual by remember { mutableStateOf("") }

    Column(
        modifier = modificador
            .fillMaxSize()
            .background(FondoOscuro)
    ) {
        VoxHeader(subtitulo = "Entrada")

        // Estado anunciado por TalkBack
        Text(
            text = mensajeEstado,
            color = TextoBlanco,
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Tarjeta principal de captura
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(340.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(NaranjaVox)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Elegir cómo ingresar el enlace",
                    onClick = {
                        alVibrarToque()
                        mostrarModalOpciones = true
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Icono del micrófono
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NaranjaVoxResaltado.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = TextoOscuroSobreNaranja,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Toca para dictar\no pegar enlace",
                    color = TextoOscuroSobreNaranja,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (cargando) {
                CircularProgressIndicator(
                    color = TextoOscuroSobreNaranja,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Indicador de estado del micrófono
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (cargando) NaranjaVox else VerdeVox)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (cargando) "Descargando artículo..." else "Listo para capturar",
                color = TextoBlanco,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.weight(1.2f))
    }

    // Opciones de entrada al tocar la tarjeta
    if (mostrarModalOpciones) {
        ModalBottomSheet(
            onDismissRequest = { mostrarModalOpciones = false },
            containerColor = SuperficieOscura,
            contentColor = TextoBlanco
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Seleccionar método de entrada",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoBlanco,
                    modifier = Modifier.semantics { heading() }
                )

                // Pegar desde el portapapeles
                Button(
                    onClick = {
                        alVibrarToque()
                        val portapapeles = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val recorte = portapapeles.primaryClip
                        val textoPegado = (if ((recorte?.itemCount ?: 0) > 0) recorte?.getItemAt(0)?.text?.toString() else null) ?: ""
                        mostrarModalOpciones = false
                        alProcesarUrl(textoPegado)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaVox),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = TextoOscuroSobreNaranja,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pegar enlace del portapapeles",
                        color = TextoOscuroSobreNaranja,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dictar por voz
                Button(
                    onClick = {
                        alVibrarToque()
                        mostrarModalOpciones = false
                        alIniciarDictado()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeVox),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = TextoBlanco,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Dictar por voz",
                        color = TextoBlanco,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Escribir el enlace manualmente
                OutlinedTextField(
                    value = textoManual,
                    onValueChange = { textoManual = it },
                    label = { Text("O escribir enlace web:") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NaranjaVox,
                        unfocusedBorderColor = VarianteSuperficieOscura,
                        focusedTextColor = TextoBlanco,
                        unfocusedTextColor = TextoBlanco
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (textoManual.isNotBlank()) {
                                    alVibrarToque()
                                    mostrarModalOpciones = false
                                    alProcesarUrl(textoManual)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Cargar enlace", tint = NaranjaVox)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
