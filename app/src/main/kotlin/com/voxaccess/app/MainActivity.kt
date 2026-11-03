package com.voxaccess.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.voxaccess.app.ui.components.VoxBottomBar
import com.voxaccess.app.ui.components.VoxPantalla
import com.voxaccess.app.ui.screens.AjustesScreen
import com.voxaccess.app.ui.screens.EntradaScreen
import com.voxaccess.app.ui.screens.ReproductorScreen
import com.voxaccess.app.ui.theme.FondoOscuro
import com.voxaccess.app.ui.theme.VoxAccessTheme
import com.voxaccess.app.ui.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val modeloVista: MainViewModel by viewModels()

    override fun onCreate(estadoInstanciaGuardado: Bundle?) {
        super.onCreate(estadoInstanciaGuardado)

        // Procesar un enlace compartido al abrir la app
        procesarIntencion(intent)

        setContent {
            VoxAccessTheme {
                val pantallaActual by modeloVista.pantallaActual.collectAsState()
                val cargando by modeloVista.cargando.collectAsState()
                val mensajeEstado by modeloVista.mensajeEstado.collectAsState()
                val articuloActivo by modeloVista.articuloActivo.collectAsState()
                val estadoReproductor by modeloVista.estadoReproductor.collectAsState()

                val velocidadInicial by modeloVista.velocidadInicial.collectAsState(initial = 1.5f)
                val hapticoActivo by modeloVista.hapticoActivo.collectAsState(initial = true)
                val vozNaturalActiva by modeloVista.vozNaturalActiva.collectAsState(initial = true)
                val vozSeleccionada by modeloVista.idVozSeleccionada.collectAsState(initial = "")
                val nombreVozMostrado by modeloVista.nombreVozMostrado.collectAsState()
                val vocesDisponibles by modeloVista.vocesDisponibles.collectAsState()
                val cargandoVoces by modeloVista.cargandoVoces.collectAsState()

                var permisoNotificacionSolicitado by rememberSaveable { mutableStateOf(false) }

                // Permiso de notificaciones para Android 13+
                val lanzadorPermisoNotificacion = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                fun asegurarPermisoNotificacion() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permisoNotificacionSolicitado) {
                        val concedido = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!concedido) {
                            permisoNotificacionSolicitado = true
                            try {
                                lanzadorPermisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                // Solicitar permiso de notificación al iniciar la primera reproducción
                LaunchedEffect(estadoReproductor.reproduciendo) {
                    if (estadoReproductor.reproduciendo) {
                        asegurarPermisoNotificacion()
                    }
                }

                // Lanzador del dictado por voz del sistema
                val lanzadorDictado = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { resultado ->
                    if (resultado.resultCode == Activity.RESULT_OK) {
                        val coincidencias = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val textoDictado = coincidencias?.firstOrNull()
                        if (!textoDictado.isNullOrBlank()) {
                            asegurarPermisoNotificacion()
                            modeloVista.procesarEnlace(textoDictado)
                        }
                    }
                }

                Scaffold(
                    containerColor = FondoOscuro,
                    bottomBar = {
                        VoxBottomBar(
                            pantallaActual = pantallaActual,
                            alSeleccionarPantalla = { nuevaPantalla ->
                                modeloVista.gestorHaptico.vibrarToqueLigero()
                                modeloVista.navegarA(nuevaPantalla)
                            }
                        )
                    }
                ) { rellenoInterno ->
                    when (pantallaActual) {
                        VoxPantalla.ENTRADA -> {
                            EntradaScreen(
                                cargando = cargando,
                                mensajeEstado = mensajeEstado,
                                alProcesarUrl = { url ->
                                    asegurarPermisoNotificacion()
                                    modeloVista.procesarEnlace(url)
                                },
                                alIniciarDictado = {
                                    val intencionDictado = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "MX"))
                                        putExtra(
                                            RecognizerIntent.EXTRA_PROMPT,
                                            "Dicta la dirección web o el tema a leer"
                                        )
                                    }
                                    try {
                                        lanzadorDictado.launch(intencionDictado)
                                    } catch (_: Exception) {
                                        // El dispositivo no tiene dictado por voz
                                    }
                                },
                                alVibrarToque = { modeloVista.gestorHaptico.vibrarToqueLigero() },
                                modificador = Modifier
                                    .fillMaxSize()
                                    .padding(rellenoInterno)
                            )
                        }

                        VoxPantalla.REPRODUCTOR -> {
                            ReproductorScreen(
                                estado = estadoReproductor,
                                tituloArticulo = articuloActivo?.titulo ?: "",
                                oraciones = articuloActivo?.oraciones ?: emptyList(),
                                alAlternarPausa = {
                                    asegurarPermisoNotificacion()
                                    modeloVista.alternarPausa()
                                },
                                alSiguienteOracion = { modeloVista.siguienteOracion() },
                                alAnteriorOracion = { modeloVista.anteriorOracion() },
                                alSaltarAOracion = { indice ->
                                    asegurarPermisoNotificacion()
                                    modeloVista.saltarAOracion(indice)
                                },
                                alAumentarVelocidad = { modeloVista.aumentarVelocidad() },
                                alDisminuirVelocidad = { modeloVista.disminuirVelocidad() },
                                alVibrarToque = { modeloVista.gestorHaptico.vibrarCambioOracion() },
                                modificador = Modifier
                                    .fillMaxSize()
                                    .padding(rellenoInterno)
                            )
                        }

                        VoxPantalla.AJUSTES -> {
                            AjustesScreen(
                                velocidadInicial = velocidadInicial,
                                hapticoActivo = hapticoActivo,
                                vozNaturalActiva = vozNaturalActiva,
                                nombreVozSeleccionada = nombreVozMostrado,
                                idVozSeleccionada = vozSeleccionada,
                                vocesDisponibles = vocesDisponibles,
                                cargandoVoces = cargandoVoces,
                                alCambiarVelocidadInicial = { nuevaVelocidad ->
                                    modeloVista.cambiarVelocidadInicial(nuevaVelocidad)
                                },
                                alCambiarHapticoActivo = { activo ->
                                    modeloVista.cambiarHapticoActivo(activo)
                                },
                                alCambiarVozNaturalActiva = { activa ->
                                    modeloVista.cambiarVozNaturalActiva(activa)
                                },
                                alSeleccionarVoz = { voz ->
                                    modeloVista.seleccionarVoz(voz)
                                },
                                alAbrirAjustesTTS = {
                                    modeloVista.abrirAjustesTTS()
                                },
                                alVibrarToque = { modeloVista.gestorHaptico.vibrarToqueLigero() },
                                modificador = Modifier
                                    .fillMaxSize()
                                    .padding(rellenoInterno)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intencion: Intent) {
        super.onNewIntent(intencion)
        setIntent(intencion)
        procesarIntencion(intencion)
    }

    private fun procesarIntencion(intencion: Intent?) {
        if (intencion == null) return
        if (intencion.action == Intent.ACTION_SEND && intencion.type == "text/plain") {
            val textoCompartido = intencion.getStringExtra(Intent.EXTRA_TEXT)
            if (!textoCompartido.isNullOrBlank()) {
                modeloVista.procesarIntentCompartido(textoCompartido)
            }
        }
    }
}
