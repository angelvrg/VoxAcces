package com.voxaccess.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxaccess.app.VoxAccessApp
import com.voxaccess.app.audio.ControladorTTS
import com.voxaccess.app.audio.EstadoReproductor
import com.voxaccess.app.audio.InformacionVoz
import com.voxaccess.app.ui.components.VoxPantalla
import com.voxaccess.core.model.ArticuloWeb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val aplicacionVox = aplicacion as VoxAccessApp
    private val repositorio = aplicacionVox.repositorioArticulos
    private val preferencias = aplicacionVox.preferencias
    val gestorHaptico = aplicacionVox.gestorHaptico

    val controladorTTS: ControladorTTS = aplicacionVox.controladorTTS

    private val _pantallaActual = MutableStateFlow(VoxPantalla.ENTRADA)
    val pantallaActual: StateFlow<VoxPantalla> = _pantallaActual.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _mensajeEstado = MutableStateFlow("Listo para capturar artículo.")
    val mensajeEstado: StateFlow<String> = _mensajeEstado.asStateFlow()

    private val _articuloActivo = MutableStateFlow<ArticuloWeb?>(null)
    val articuloActivo: StateFlow<ArticuloWeb?> = _articuloActivo.asStateFlow()

    val estadoReproductor: StateFlow<EstadoReproductor> = controladorTTS.estado

    // Preferencias
    val velocidadInicial = preferencias.flujoVelocidadInicial
    val hapticoActivo = preferencias.flujoHapticoActivo
    val vozNaturalActiva = preferencias.flujoVozNaturalActiva
    val idVozSeleccionada = preferencias.flujoVozSeleccionada
    val vozSeleccionada = idVozSeleccionada

    val vocesDisponibles: StateFlow<List<InformacionVoz>> = controladorTTS.vocesInfoDisponibles

    val cargandoVoces: StateFlow<Boolean> = controladorTTS.ttsInicializadoFlujo
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val nombreVozMostrado: StateFlow<String> = combine(
        preferencias.flujoVozSeleccionada,
        controladorTTS.vocesInfoDisponibles,
        controladorTTS.ttsInicializadoFlujo
    ) { idVoz, _, inicializado ->
        if (!inicializado) {
            "Cargando voz del sistema…"
        } else {
            controladorTTS.resolverNombreAmigable(idVoz)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Cargando voz del sistema…"
    )

    init {
        // Recuperar el último artículo guardado si aún no hay uno cargado en el motor
        repositorio.obtenerArticuloActivo()?.let { guardado ->
            _articuloActivo.value = guardado
            if (controladorTTS.estado.value.totalOraciones == 0) {
                controladorTTS.cargarOraciones(guardado.oraciones, guardado.idioma)
            }
        }

        viewModelScope.launch {
            hapticoActivo.collect { activo ->
                gestorHaptico.habilitado = activo
            }
        }

        viewModelScope.launch {
            velocidadInicial.collect { velocidad ->
                if (!controladorTTS.estado.value.reproduciendo) {
                    controladorTTS.cambiarVelocidad(velocidad)
                }
            }
        }

        viewModelScope.launch {
            combine(idVozSeleccionada, vozNaturalActiva) { voz, natural ->
                voz to natural
            }.collect { (voz, natural) ->
                controladorTTS.configurarVozPreferida(voz, natural)
            }
        }
    }

    fun navegarA(pantalla: VoxPantalla) {
        _pantallaActual.value = pantalla
    }

    fun procesarEnlace(urlOTexto: String, reproducirDeInmediato: Boolean = true) {
        if (_cargando.value) return

        _cargando.value = true
        _mensajeEstado.value = "Descargando y extrayendo artículo principal..."

        viewModelScope.launch {
            val resultado = repositorio.procesarYGuardar(urlOTexto)
            _cargando.value = false

            resultado.onSuccess { articulo ->
                _articuloActivo.value = articulo
                _mensajeEstado.value = "Artículo listo: ${articulo.titulo}"

                controladorTTS.cargarOraciones(
                    lista = articulo.oraciones,
                    idioma = articulo.idioma,
                    velocidadInicial = estadoReproductor.value.velocidad
                )

                _pantallaActual.value = VoxPantalla.REPRODUCTOR

                if (reproducirDeInmediato) {
                    controladorTTS.reproducir()
                }
            }.onFailure { error ->
                _mensajeEstado.value = error.message ?: "Ocurrió un error al procesar el enlace."
            }
        }
    }

    fun procesarIntentCompartido(textoCompartido: String) {
        // Abrir y reproducir el enlace compartido de inmediato
        procesarEnlace(textoCompartido, reproducirDeInmediato = true)
    }

    fun alternarPausa() {
        controladorTTS.alternarPausaReanudar()
    }

    fun siguienteOracion() {
        controladorTTS.siguienteOracion()
    }

    fun anteriorOracion() {
        controladorTTS.anteriorOracion()
    }

    fun saltarAOracion(indice: Int) {
        controladorTTS.saltarAOracion(indice)
    }

    fun aumentarVelocidad() {
        controladorTTS.aumentarVelocidad()
    }

    fun disminuirVelocidad() {
        controladorTTS.disminuirVelocidad()
    }

    fun cambiarVelocidadInicial(nueva: Float) {
        viewModelScope.launch {
            preferencias.guardarVelocidad(nueva)
            controladorTTS.cambiarVelocidad(nueva)
        }
    }

    fun cambiarHapticoActivo(activo: Boolean) {
        viewModelScope.launch {
            preferencias.guardarHapticoActivo(activo)
        }
    }

    fun cambiarVozNaturalActiva(activa: Boolean) {
        viewModelScope.launch {
            preferencias.guardarVozNaturalActiva(activa)
        }
    }

    fun seleccionarVoz(idVoz: String) {
        viewModelScope.launch {
            preferencias.guardarVozSeleccionada(idVoz)
        }
    }

    fun abrirAjustesTTS() {
        val contexto = getApplication<Application>()
        val intencion = Intent("com.android.settings.TTS_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            contexto.startActivity(intencion)
        } catch (_: Exception) {
            try {
                contexto.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
            }
        }
    }
}
