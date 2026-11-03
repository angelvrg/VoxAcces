package com.voxaccess.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.voxaccess.core.model.Oracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class EstadoReproductor(
    val reproduciendo: Boolean = false,
    val pausado: Boolean = false,
    val indiceOracionActual: Int = 0,
    val totalOraciones: Int = 0,
    val oracionActualTexto: String = "",
    val velocidad: Float = 1.5f,
    val idiomaActual: String = "es",
    val vozActualNombre: String = "Predeterminada"
)

class ControladorTTS(
    private val contexto: Context,
    private val alCambiarEstado: ((EstadoReproductor) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var motorTts: TextToSpeech? = null
    private var ttsInicializado = false
    private var reproduccionPendienteAlIniciar = false

    private val gestorAudio = contexto.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var solicitudFocoAudio: AudioFocusRequest? = null

    private var oraciones: List<Oracion> = emptyList()
    private var indiceActual = 0
    private var velocidadActual = 1.5f
    private var idiomaActual = "es"
    private var usarVozNatural = true
    private var nombreVozPreferida: String? = null

    private val _estado = MutableStateFlow(EstadoReproductor())
    val estado: StateFlow<EstadoReproductor> = _estado.asStateFlow()

    private val _ttsInicializado = MutableStateFlow(false)
    val ttsInicializadoFlujo: StateFlow<Boolean> = _ttsInicializado.asStateFlow()

    private val _vocesDisponibles = MutableStateFlow<List<Voice>>(emptyList())
    val vocesDisponibles: StateFlow<List<Voice>> = _vocesDisponibles.asStateFlow()

    private val _vocesInfoDisponibles = MutableStateFlow<List<InformacionVoz>>(emptyList())
    val vocesInfoDisponibles: StateFlow<List<InformacionVoz>> = _vocesInfoDisponibles.asStateFlow()

    private val receptorAuriculares = object : BroadcastReceiver() {
        override fun onReceive(contextoReceptor: Context?, intencion: Intent?) {
            if (intencion?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Pausar si se desconectan los auriculares
                pausar()
            }
        }
    }

    init {
        motorTts = TextToSpeech(contexto.applicationContext, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            contexto.registerReceiver(
                receptorAuriculares,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            contexto.registerReceiver(
                receptorAuriculares,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
        }
    }

    override fun onInit(estadoInicializacion: Int) {
        if (estadoInicializacion == TextToSpeech.SUCCESS) {
            ttsInicializado = true
            _ttsInicializado.value = true

            motorTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(identificadorLocucion: String?) {
                    actualizarEstado(reproduciendo = true, pausado = false)
                }

                override fun onDone(identificadorLocucion: String?) {
                    // Validar identificador para evitar saltos por cancelaciones previas
                    if (identificadorLocucion != "oracion_$indiceActual") return

                    if (_estado.value.reproduciendo && !_estado.value.pausado) {
                        if (indiceActual + 1 < oraciones.size) {
                            indiceActual++
                            reproducirOracionEnIndice(indiceActual)
                        } else {
                            actualizarEstado(reproduciendo = false, pausado = false)
                            liberarFocoAudio()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(identificadorLocucion: String?) {
                    actualizarEstado(reproduciendo = false, pausado = false)
                }
            })

            actualizarVocesDisponibles()
            configurarIdiomaYVoz(idiomaActual)

            if (reproduccionPendienteAlIniciar && oraciones.isNotEmpty()) {
                reproduccionPendienteAlIniciar = false
                reproducir()
            }
        }
    }

    fun obtenerIdiomaSistema(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contexto.resources.configuration.locales.get(0) ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            contexto.resources.configuration.locale ?: Locale.getDefault()
        }
    }

    fun actualizarVocesDisponibles() {
        if (!ttsInicializado) return
        try {
            val idiomaSistema = obtenerIdiomaSistema()
            val todasLasVoces = motorTts?.voices?.filter { voz ->
                voz.locale.language.equals(idiomaSistema.language, ignoreCase = true)
            } ?: emptyList()

            val vocesLocales = todasLasVoces.filter { !it.isNetworkConnectionRequired }
            val vocesFiltradas = if (vocesLocales.isNotEmpty()) vocesLocales else todasLasVoces
            _vocesDisponibles.value = vocesFiltradas

            val vozPredeterminadaSistema = motorTts?.defaultVoice

            val resultado = mutableListOf<InformacionVoz>()
            val porRegion = vocesFiltradas.groupBy { it.locale.country }

            porRegion.forEach { (_, listaDeRegion) ->
                val listaOrdenada = listaDeRegion.sortedBy { it.name }
                listaOrdenada.forEachIndexed { indice, voz ->
                    val nombrePais = voz.locale.getDisplayCountry(idiomaSistema)
                        .ifBlank { voz.locale.displayCountry }
                    val nombreIdioma = voz.locale.getDisplayLanguage(idiomaSistema)
                        .ifBlank { voz.locale.displayLanguage }
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(idiomaSistema) else it.toString() }

                    val idiomaPais = if (nombrePais.isNotBlank()) {
                        "$nombreIdioma ($nombrePais)"
                    } else {
                        nombreIdioma
                    }

                    val nombreAmigable = "$idiomaPais · Voz ${indice + 1}"
                    val esNatural = voz.quality >= Voice.QUALITY_HIGH
                    val esPredeterminada = (voz.name == vozPredeterminadaSistema?.name)

                    resultado.add(
                        InformacionVoz(
                            id = voz.name,
                            nombreAmigable = nombreAmigable,
                            idiomaPais = idiomaPais,
                            esNatural = esNatural,
                            esPredeterminada = esPredeterminada
                        )
                    )
                }
            }

            _vocesInfoDisponibles.value = resultado
        } catch (_: Exception) {
            _vocesDisponibles.value = emptyList()
            _vocesInfoDisponibles.value = emptyList()
        }
    }

    fun resolverNombreAmigable(idVoz: String?): String {
        if (!ttsInicializado) {
            return "Cargando voz del sistema…"
        }

        val lista = _vocesInfoDisponibles.value

        if (!idVoz.isNullOrBlank() && !idVoz.contains("Lucía", ignoreCase = true)) {
            val encontrada = lista.firstOrNull { it.id.equals(idVoz, ignoreCase = true) }
            if (encontrada != null) {
                return encontrada.nombreAmigable
            }
        }

        val vozPredeterminada = lista.firstOrNull { it.esPredeterminada } ?: lista.firstOrNull()
        return if (vozPredeterminada != null) {
            vozPredeterminada.nombreAmigable
        } else {
            "Predeterminada del sistema"
        }
    }

    fun cargarOraciones(lista: List<Oracion>, idioma: String, velocidadInicial: Float = 1.5f) {
        // Detener cualquier lectura previa
        reproduccionPendienteAlIniciar = false
        motorTts?.stop()
        this.oraciones = lista
        this.idiomaActual = idioma
        this.velocidadActual = velocidadInicial.coerceIn(0.75f, 2.5f)
        this.indiceActual = 0

        configurarIdiomaYVoz(idioma)
        actualizarEstado(
            reproduciendo = false,
            pausado = false,
            indiceActual = 0,
            totalOraciones = lista.size,
            oracionActualTexto = lista.firstOrNull()?.textoPlano ?: "",
            velocidad = this.velocidadActual,
            idiomaActual = idioma
        )
    }

    fun reproducir() {
        if (oraciones.isEmpty()) return
        if (!ttsInicializado) {
            reproduccionPendienteAlIniciar = true
            return
        }

        if (!solicitarFocoAudio()) return

        reproducirOracionEnIndice(indiceActual)
    }

    fun pausar() {
        reproduccionPendienteAlIniciar = false
        if (!ttsInicializado) return
        motorTts?.stop()
        actualizarEstado(reproduciendo = false, pausado = true)
        liberarFocoAudio()
    }

    fun alternarPausaReanudar() {
        if (_estado.value.reproduciendo) {
            pausar()
        } else {
            reproducir()
        }
    }

    fun siguienteOracion() {
        if (indiceActual + 1 < oraciones.size) {
            indiceActual++
            if (_estado.value.reproduciendo) {
                reproducirOracionEnIndice(indiceActual)
            } else {
                actualizarEstado(
                    indiceActual = indiceActual,
                    oracionActualTexto = oraciones[indiceActual].textoPlano
                )
            }
        }
    }

    fun anteriorOracion() {
        if (indiceActual > 0) {
            indiceActual--
            if (_estado.value.reproduciendo) {
                reproducirOracionEnIndice(indiceActual)
            } else {
                actualizarEstado(
                    indiceActual = indiceActual,
                    oracionActualTexto = oraciones[indiceActual].textoPlano
                )
            }
        }
    }

    fun saltarAOracion(indice: Int) {
        if (indice in oraciones.indices) {
            indiceActual = indice
            if (!solicitarFocoAudio()) {
                actualizarEstado(
                    indiceActual = indiceActual,
                    oracionActualTexto = oraciones[indiceActual].textoPlano
                )
                return
            }
            reproducirOracionEnIndice(indiceActual)
        }
    }

    fun cambiarVelocidad(nuevaVelocidad: Float) {
        velocidadActual = nuevaVelocidad.coerceIn(0.75f, 2.5f)
        motorTts?.setSpeechRate(velocidadActual)
        actualizarEstado(velocidad = velocidadActual)
        if (_estado.value.reproduciendo) {
            reproducirOracionEnIndice(indiceActual)
        }
    }

    fun aumentarVelocidad() {
        cambiarVelocidad((velocidadActual + 0.25f).coerceAtMost(2.5f))
    }

    fun disminuirVelocidad() {
        cambiarVelocidad((velocidadActual - 0.25f).coerceAtLeast(0.75f))
    }

    fun configurarVozPreferida(nombreVoz: String?, usarNatural: Boolean) {
        val limpia = nombreVoz?.takeIf { it.isNotBlank() && !it.contains("Lucía", ignoreCase = true) }
        val idiomaSistema = obtenerIdiomaSistema().language

        this.nombreVozPreferida = if (limpia != null) {
            val vozValida = motorTts?.voices?.firstOrNull {
                it.name.equals(limpia, ignoreCase = true) &&
                        it.locale.language.equals(idiomaSistema, ignoreCase = true)
            }
            vozValida?.name ?: if (motorTts == null) limpia else null
        } else null

        this.usarVozNatural = usarNatural
        configurarIdiomaYVoz(idiomaActual)
    }

    fun obtenerVocesDisponibles(): List<Voice> {
        return _vocesDisponibles.value
    }

    private fun reproducirOracionEnIndice(indice: Int) {
        val oracion = oraciones.getOrNull(indice) ?: return

        motorTts?.stop()
        motorTts?.setSpeechRate(velocidadActual)

        val parametros = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val identificadorLocucion = "oracion_$indice"
        motorTts?.speak(oracion.textoPlano, TextToSpeech.QUEUE_FLUSH, parametros, identificadorLocucion)

        actualizarEstado(
            reproduciendo = true,
            pausado = false,
            indiceActual = indice,
            oracionActualTexto = oracion.textoPlano
        )
    }

    private fun configurarIdiomaYVoz(codigoIdioma: String) {
        if (!ttsInicializado) return

        val idiomaSistema = obtenerIdiomaSistema()
        val configuracionRegional = if (codigoIdioma.startsWith("en", ignoreCase = true)) {
            Locale.ENGLISH
        } else if (codigoIdioma.startsWith("es", ignoreCase = true)) {
            if (idiomaSistema.language.equals("es", ignoreCase = true)) {
                idiomaSistema
            } else {
                Locale("es", "MX")
            }
        } else {
            Locale(codigoIdioma)
        }

        motorTts?.language = configuracionRegional

        try {
            val voces = motorTts?.voices ?: return

            // 1. Voz preferida elegida por el usuario si coincide con el idioma del contenido
            val vozSeleccionada = if (!nombreVozPreferida.isNullOrBlank()) {
                voces.firstOrNull {
                    it.locale.language.equals(configuracionRegional.language, ignoreCase = true) &&
                            it.name.equals(nombreVozPreferida, ignoreCase = true)
                }
            } else null

            // 2. Si no hay voz preferida o no coincide el idioma, y el texto es del idioma del sistema:
            // intentar usar la voz predeterminada del motor en el teléfono
            val vozPredeterminadaSistema = if (configuracionRegional.language.equals(idiomaSistema.language, ignoreCase = true)) {
                motorTts?.defaultVoice?.takeIf {
                    it.locale.language.equals(configuracionRegional.language, ignoreCase = true)
                }
            } else null

            // 3. Fallback a voz natural (calidad alta) o primera voz disponible del idioma
            val vozFinal = vozSeleccionada ?: vozPredeterminadaSistema ?: voces.firstOrNull { voz ->
                voz.locale.language.equals(configuracionRegional.language, ignoreCase = true) &&
                        (!usarVozNatural || voz.quality >= Voice.QUALITY_HIGH)
            } ?: voces.firstOrNull { voz ->
                voz.locale.language.equals(configuracionRegional.language, ignoreCase = true)
            }

            if (vozFinal != null) {
                motorTts?.voice = vozFinal
                val nombreAmigable = resolverNombreAmigable(vozFinal.name)
                actualizarEstado(vozActualNombre = nombreAmigable)
            }
        } catch (_: Exception) {
            // Motores TTS sin soporte de voces
        }
    }

    private fun solicitarFocoAudio(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val solicitud = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener { cambioFoco ->
                    when (cambioFoco) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausar()
                    }
                }
                .build()
            solicitudFocoAudio = solicitud
            gestorAudio.requestAudioFocus(solicitud) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            gestorAudio.requestAudioFocus(
                { cambioFoco ->
                    if (cambioFoco == AudioManager.AUDIOFOCUS_LOSS ||
                        cambioFoco == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    ) {
                        pausar()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun liberarFocoAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            solicitudFocoAudio?.let { gestorAudio.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            gestorAudio.abandonAudioFocus(null)
        }
    }

    private fun actualizarEstado(
        reproduciendo: Boolean = _estado.value.reproduciendo,
        pausado: Boolean = _estado.value.pausado,
        indiceActual: Int = this.indiceActual,
        totalOraciones: Int = this.oraciones.size,
        oracionActualTexto: String = this.oraciones.getOrNull(indiceActual)?.textoPlano ?: "",
        velocidad: Float = this.velocidadActual,
        idiomaActual: String = this.idiomaActual,
        vozActualNombre: String = _estado.value.vozActualNombre
    ) {
        val nuevo = EstadoReproductor(
            reproduciendo = reproduciendo,
            pausado = pausado,
            indiceOracionActual = indiceActual,
            totalOraciones = totalOraciones,
            oracionActualTexto = oracionActualTexto,
            velocidad = velocidad,
            idiomaActual = idiomaActual,
            vozActualNombre = vozActualNombre
        )
        _estado.value = nuevo
        alCambiarEstado?.invoke(nuevo)
    }

    fun liberar() {
        try {
            contexto.unregisterReceiver(receptorAuriculares)
        } catch (_: Exception) {
        }
        liberarFocoAudio()
        motorTts?.stop()
        motorTts?.shutdown()
        motorTts = null
    }
}
