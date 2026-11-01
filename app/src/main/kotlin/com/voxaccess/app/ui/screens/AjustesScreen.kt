package com.voxaccess.app.ui.screens

import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxaccess.app.ui.components.VoxHeader
import com.voxaccess.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    velocidadInicial: Float,
    hapticoActivo: Boolean,
    vozNaturalActiva: Boolean,
    nombreVozSeleccionada: String,
    vocesDisponibles: List<Voice>,
    alCambiarVelocidadInicial: (Float) -> Unit,
    alCambiarHapticoActivo: (Boolean) -> Unit,
    alCambiarVozNaturalActiva: (Boolean) -> Unit,
    alSeleccionarVoz: (String) -> Unit,
    alVibrarToque: () -> Unit,
    modificador: Modifier = Modifier
) {
    var mostrarSelectorVoces by remember { mutableStateOf(false) }

    Column(
        modifier = modificador
            .fillMaxSize()
            .background(FondoOscuro)
    ) {
        VoxHeader(subtitulo = "Ajustes")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Voz y accesibilidad",
                color = TextoBlanco,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .semantics { heading() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Voz natural
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Voz natural",
                    color = TextoBlanco,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clickable(role = Role.Button, onClickLabel = "Cambiar de voz") {
                                alVibrarToque()
                                mostrarSelectorVoces = true
                            }
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Voz $nombreVozSeleccionada"
                            }
                    ) {
                        Text(
                            text = nombreVozSeleccionada,
                            color = TextoBlanco,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Switch(
                        checked = vozNaturalActiva,
                        onCheckedChange = { nuevoEstado ->
                            alVibrarToque()
                            alCambiarVozNaturalActiva(nuevoEstado)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextoOscuroSobreNaranja,
                            checkedTrackColor = VerdeVox,
                            uncheckedThumbColor = TextoGris,
                            uncheckedTrackColor = VarianteSuperficieOscura
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Voz natural"
                            stateDescription = if (vozNaturalActiva) "Activada" else "Desactivada"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = VarianteSuperficieOscura, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // Velocidad inicial
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Velocidad inicial",
                    color = TextoBlanco,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Botón para bajar la velocidad
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(NaranjaVox)
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    alVibrarToque()
                                    val nuevaVelocidad = (velocidadInicial - 0.25f).coerceAtLeast(0.75f)
                                    alCambiarVelocidadInicial(nuevaVelocidad)
                                }
                            )
                            .semantics {
                                contentDescription = "Disminuir velocidad inicial"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = TextoOscuroSobreNaranja,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Valor actual de la velocidad
                    Text(
                        text = String.format("%.2fx", velocidadInicial).replace(".00x", ".0x"),
                        color = TextoBlanco,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics {
                            contentDescription = "Velocidad inicial actual: ${String.format("%.2f", velocidadInicial)} por"
                        }
                    )

                    // Botón para subir la velocidad
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(NaranjaVox)
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    alVibrarToque()
                                    val nuevaVelocidad = (velocidadInicial + 0.25f).coerceAtMost(2.5f)
                                    alCambiarVelocidadInicial(nuevaVelocidad)
                                }
                            )
                            .semantics {
                                contentDescription = "Aumentar velocidad inicial"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = TextoOscuroSobreNaranja,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = VarianteSuperficieOscura, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // Confirmación háptica
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Confirmación háptica",
                    color = TextoBlanco,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (hapticoActivo) "Activada" else "Desactivada",
                        color = TextoBlanco,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Switch(
                        checked = hapticoActivo,
                        onCheckedChange = { activo ->
                            alVibrarToque()
                            alCambiarHapticoActivo(activo)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextoOscuroSobreNaranja,
                            checkedTrackColor = VerdeVox,
                            uncheckedThumbColor = TextoGris,
                            uncheckedTrackColor = VarianteSuperficieOscura
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Confirmación háptica"
                            stateDescription = if (hapticoActivo) "Activada" else "Desactivada"
                        }
                    )
                }
            }
        }
    }

    // Selector de voces instaladas
    if (mostrarSelectorVoces) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorVoces = false },
            containerColor = SuperficieOscura,
            contentColor = TextoBlanco
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Seleccionar voz del sistema",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoBlanco,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (vocesDisponibles.isEmpty()) {
                    Text(
                        text = "Voz estándar de Android (Lucía · Español)",
                        color = TextoGris,
                        fontSize = 16.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vocesDisponibles) { voz ->
                            val esSeleccionada = voz.name.contains(nombreVozSeleccionada, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (esSeleccionada) NaranjaVox else VarianteSuperficieOscura)
                                    .clickable(role = Role.Button) {
                                        alVibrarToque()
                                        alSeleccionarVoz(voz.name)
                                        mostrarSelectorVoces = false
                                    }
                                    .semantics { selected = esSeleccionada }
                                    .heightIn(min = 56.dp)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${voz.name} (${voz.locale.displayLanguage})",
                                    color = if (esSeleccionada) TextoOscuroSobreNaranja else TextoBlanco,
                                    fontSize = 16.sp,
                                    fontWeight = if (esSeleccionada) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
