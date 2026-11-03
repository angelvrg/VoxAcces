package com.voxaccess.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxaccess.app.audio.InformacionVoz
import com.voxaccess.app.ui.components.VoxHeader
import com.voxaccess.app.ui.theme.*

// Pantalla de configuración de voz, velocidad y confirmación háptica
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    velocidadInicial: Float,
    hapticoActivo: Boolean,
    vozNaturalActiva: Boolean,
    nombreVozSeleccionada: String,
    idVozSeleccionada: String,
    vocesDisponibles: List<InformacionVoz>,
    cargandoVoces: Boolean,
    alCambiarVelocidadInicial: (Float) -> Unit,
    alCambiarHapticoActivo: (Boolean) -> Unit,
    alCambiarVozNaturalActiva: (Boolean) -> Unit,
    alSeleccionarVoz: (String) -> Unit,
    alAbrirAjustesTTS: () -> Unit,
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
                                contentDescription = "Voz actual: $nombreVozSeleccionada. Toca dos veces para cambiar."
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
                            contentDescription = "Preferencia de voz natural"
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Solo se muestran las voces instaladas para el idioma del dispositivo.",
                    fontSize = 14.sp,
                    color = TextoGris
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (cargandoVoces) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NaranjaVox,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Consultando voces instaladas…",
                            color = TextoGris,
                            fontSize = 16.sp
                        )
                    }
                } else if (vocesDisponibles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Voz predeterminada del sistema activa.",
                            color = TextoBlanco,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "No se detectaron voces locales descargadas para este idioma. Puedes instalar voces de alta calidad desde los ajustes de síntesis de voz de Android.",
                            color = TextoGris,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = {
                                alVibrarToque()
                                alAbrirAjustesTTS()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NaranjaVox,
                                contentColor = TextoOscuroSobreNaranja
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Abrir ajustes de texto a voz",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vocesDisponibles) { voz ->
                            val esSeleccionada = voz.id.equals(idVozSeleccionada, ignoreCase = true) ||
                                    (idVozSeleccionada.isBlank() && voz.esPredeterminada)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (esSeleccionada) NaranjaVox else VarianteSuperficieOscura)
                                    .clickable(role = Role.RadioButton) {
                                        alVibrarToque()
                                        alSeleccionarVoz(voz.id)
                                        mostrarSelectorVoces = false
                                    }
                                    .semantics {
                                        selected = esSeleccionada
                                        contentDescription = buildString {
                                            append(voz.nombreAmigable)
                                            if (voz.esNatural) append(", voz natural de alta calidad")
                                            if (voz.esPredeterminada) append(", voz predeterminada del sistema")
                                            if (esSeleccionada) append(", seleccionada")
                                        }
                                    }
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = voz.nombreAmigable,
                                        color = if (esSeleccionada) TextoOscuroSobreNaranja else TextoBlanco,
                                        fontSize = 16.sp,
                                        fontWeight = if (esSeleccionada) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (voz.esPredeterminada) {
                                        Text(
                                            text = "Predeterminada del sistema",
                                            color = if (esSeleccionada) TextoOscuroSobreNaranja.copy(alpha = 0.8f) else TextoGris,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (voz.esNatural) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (esSeleccionada) TextoOscuroSobreNaranja else VerdeVox,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Natural",
                                            color = if (esSeleccionada) TextoBlanco else TextoOscuroSobreNaranja,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
