package com.voxaccess.app.ui.screens

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxaccess.app.audio.EstadoReproductor
import com.voxaccess.app.ui.components.VoxHeader
import com.voxaccess.app.ui.theme.*
import com.voxaccess.core.model.Oracion
import kotlin.math.abs

/**
 * Indica si un lector de pantalla (TalkBack) con exploración táctil está activo.
 * En ese caso TalkBack intercepta los toques y deslizamientos de un dedo, así que
 * los gestos propios se desactivan y se usan acciones de accesibilidad y botones.
 */
@Composable
private fun recordarExploracionTactilActiva(): Boolean {
    val contexto = LocalContext.current
    val gestorAccesibilidad = remember {
        contexto.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    var activa by remember { mutableStateOf(gestorAccesibilidad.isEnabled && gestorAccesibilidad.isTouchExplorationEnabled) }
    DisposableEffect(gestorAccesibilidad) {
        val oyente = AccessibilityManager.TouchExplorationStateChangeListener { habilitada ->
            activa = gestorAccesibilidad.isEnabled && habilitada
        }
        gestorAccesibilidad.addTouchExplorationStateChangeListener(oyente)
        onDispose { gestorAccesibilidad.removeTouchExplorationStateChangeListener(oyente) }
    }
    return activa
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReproductorScreen(
    estado: EstadoReproductor,
    tituloArticulo: String,
    oraciones: List<Oracion>,
    alAlternarPausa: () -> Unit,
    alSiguienteOracion: () -> Unit,
    alAnteriorOracion: () -> Unit,
    alSaltarAOracion: (Int) -> Unit,
    alAumentarVelocidad: () -> Unit,
    alDisminuirVelocidad: () -> Unit,
    alVibrarToque: () -> Unit,
    modificador: Modifier = Modifier
) {
    val estaReproduciendo = estado.reproduciendo
    val velocidadFormateada = String.format("%.2fx", estado.velocidad)
        .replace(".00x", ".0x")
    val textoEstado = if (estaReproduciendo) "Reproduciendo" else "Pausado"

    val lectorActivo = recordarExploracionTactilActiva()
    var mostrarListaOraciones by remember { mutableStateOf(false) }

    // Umbral de deslizamiento independiente de la densidad de pantalla
    val umbralDeslizamiento = with(LocalDensity.current) { 56.dp.toPx() }

    Column(
        modifier = modificador
            .fillMaxSize()
            .background(FondoOscuro)
    ) {
        VoxHeader(subtitulo = "Reproductor")

        // Indicador del estado de reproducción y botón para abrir la lista de oraciones
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (estaReproduciendo) VerdeVox else VarianteSuperficieOscura)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$textoEstado, velocidad $velocidadFormateada"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$textoEstado ($velocidadFormateada)",
                    color = if (estaReproduciendo) TextoOscuroSobreNaranja else TextoBlanco,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (oraciones.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(VarianteSuperficieOscura)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Ver lista de oraciones",
                            onClick = {
                                alVibrarToque()
                                mostrarListaOraciones = true
                            }
                        )
                        .semantics {
                            contentDescription = "Lista de oraciones (${oraciones.size})"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        tint = NaranjaVox,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Área de lectura. Gestos solo si no hay lector de pantalla activo;
        // con lector de pantalla se exponen como acciones de accesibilidad.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SuperficieOscura)
                .then(
                    if (lectorActivo) {
                        Modifier
                    } else {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        alVibrarToque()
                                        alAlternarPausa()
                                    }
                                )
                            }
                            .pointerInput(umbralDeslizamiento) {
                                var desplazamientoX = 0f
                                var desplazamientoY = 0f
                                detectDragGestures(
                                    onDrag = { cambio, cantidadArrastre ->
                                        cambio.consume()
                                        desplazamientoX += cantidadArrastre.x
                                        desplazamientoY += cantidadArrastre.y
                                    },
                                    onDragEnd = {
                                        if (abs(desplazamientoX) > abs(desplazamientoY)) {
                                            if (desplazamientoX > umbralDeslizamiento) {
                                                alVibrarToque()
                                                alAnteriorOracion()
                                            } else if (desplazamientoX < -umbralDeslizamiento) {
                                                alVibrarToque()
                                                alSiguienteOracion()
                                            }
                                        } else {
                                            if (desplazamientoY < -umbralDeslizamiento) {
                                                alVibrarToque()
                                                alAumentarVelocidad()
                                            } else if (desplazamientoY > umbralDeslizamiento) {
                                                alVibrarToque()
                                                alDisminuirVelocidad()
                                            }
                                        }
                                        desplazamientoX = 0f
                                        desplazamientoY = 0f
                                    },
                                    onDragCancel = {
                                        desplazamientoX = 0f
                                        desplazamientoY = 0f
                                    }
                                )
                            }
                    }
                )
                .semantics(mergeDescendants = true) {
                    stateDescription = textoEstado
                    onClick(label = if (estaReproduciendo) "Pausar lectura" else "Reanudar lectura") {
                        alAlternarPausa()
                        true
                    }
                    customActions = buildList {
                        add(CustomAccessibilityAction("Oración siguiente") { alSiguienteOracion(); true })
                        add(CustomAccessibilityAction("Oración anterior") { alAnteriorOracion(); true })
                        add(CustomAccessibilityAction("Aumentar velocidad") { alAumentarVelocidad(); true })
                        add(CustomAccessibilityAction("Disminuir velocidad") { alDisminuirVelocidad(); true })
                        if (oraciones.isNotEmpty()) {
                            add(CustomAccessibilityAction("Abrir lista de oraciones") {
                                mostrarListaOraciones = true
                                true
                            })
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = TextoGris,
                    modifier = Modifier.size(32.dp)
                )

                // Oración activa (sin liveRegion: TalkBack no debe leerla encima de la voz de la app)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (tituloArticulo.isNotBlank()) {
                        Text(
                            text = tituloArticulo,
                            color = NaranjaVox,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = estado.oracionActualTexto.ifBlank {
                            "Aún no hay artículo. Ve a la pestaña Entrada."
                        },
                        color = TextoBlanco,
                        fontSize = if (estado.oracionActualTexto.length > 80) 22.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    if (estado.totalOraciones > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Oración ${estado.indiceOracionActual + 1} de ${estado.totalOraciones}",
                            color = TextoGris,
                            fontSize = 16.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = TextoGris,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Controles con botones: alternativa a los gestos (lector de pantalla, Switch Access, etc.)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonControl(Icons.Default.Remove, "Disminuir velocidad", VarianteSuperficieOscura, TextoBlanco, Modifier.weight(1f)) {
                alVibrarToque(); alDisminuirVelocidad()
            }
            BotonControl(Icons.Default.SkipPrevious, "Oración anterior", VarianteSuperficieOscura, TextoBlanco, Modifier.weight(1f)) {
                alVibrarToque(); alAnteriorOracion()
            }
            BotonControl(
                if (estaReproduciendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (estaReproduciendo) "Pausar lectura" else "Reanudar lectura",
                NaranjaVox, TextoOscuroSobreNaranja, Modifier.weight(1.3f)
            ) {
                alVibrarToque(); alAlternarPausa()
            }
            BotonControl(Icons.Default.SkipNext, "Oración siguiente", VarianteSuperficieOscura, TextoBlanco, Modifier.weight(1f)) {
                alVibrarToque(); alSiguienteOracion()
            }
            BotonControl(Icons.Default.Add, "Aumentar velocidad", VarianteSuperficieOscura, TextoBlanco, Modifier.weight(1f)) {
                alVibrarToque(); alAumentarVelocidad()
            }
        }

        // Ayuda visual de gestos (oculta al lector: ya existen acciones y botones etiquetados)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 8.dp)
                .clearAndSetSemantics { }
        ) {
            Text("Un toque: pausa o reanuda", color = TextoBlanco, fontSize = 16.sp)
            Text("Deslizar izq/der: oración siguiente/anterior", color = TextoBlanco, fontSize = 16.sp)
            Text("Deslizar arriba/abajo: velocidad", color = TextoBlanco, fontSize = 16.sp)
        }
    }

    // Modal con la lista completa de oraciones para saltar a una frase exacta (HU-07 / CP-15)
    if (mostrarListaOraciones && oraciones.isNotEmpty()) {
        val estadoLista = rememberLazyListState(
            initialFirstVisibleItemIndex = estado.indiceOracionActual.coerceIn(0, oraciones.lastIndex)
        )
        ModalBottomSheet(
            onDismissRequest = { mostrarListaOraciones = false },
            containerColor = SuperficieOscura,
            contentColor = TextoBlanco
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Oraciones del artículo (${oraciones.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoBlanco,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    state = estadoLista,
                    modifier = Modifier.fillMaxHeight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(oraciones) { indice, oracion ->
                        val activa = indice == estado.indiceOracionActual
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activa) NaranjaVox else VarianteSuperficieOscura)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Leer desde la oración ${indice + 1}"
                                ) {
                                    alVibrarToque()
                                    alSaltarAOracion(indice)
                                    mostrarListaOraciones = false
                                }
                                .semantics {
                                    selected = activa
                                    contentDescription = "Oración ${indice + 1}: ${oracion.textoPlano}"
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${indice + 1}. ${oracion.textoPlano}",
                                color = if (activa) TextoOscuroSobreNaranja else TextoBlanco,
                                fontSize = 16.sp,
                                fontWeight = if (activa) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BotonControl(
    icono: ImageVector,
    descripcion: String,
    fondo: Color,
    tinte: Color,
    modificador: Modifier = Modifier,
    alHacerClic: () -> Unit
) {
    Box(
        modifier = modificador
            .heightIn(min = 56.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fondo)
            .clickable(role = Role.Button, onClickLabel = descripcion, onClick = alHacerClic)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = tinte, modifier = Modifier.size(30.dp))
    }
}
