package com.voxaccess.app.audio

import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

// Adaptador Media3 que expone el reproductor a la sesión multimedia del sistema
@UnstableApi
class TtsMediaPlayer(
    private val controladorTTS: ControladorTTS
) : SimpleBasePlayer(Looper.getMainLooper()) {

    private var tituloArticulo: String = "VoxAccess"

    fun actualizarDesdeEstado() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidateState()
        } else {
            android.os.Handler(Looper.getMainLooper()).post {
                invalidateState()
            }
        }
    }

    fun fijarTituloArticulo(titulo: String) {
        this.tituloArticulo = titulo.ifBlank { "VoxAccess" }
        actualizarDesdeEstado()
    }

    override fun getState(): State {
        val estadoActual = controladorTTS.estado.value
        val comandos = Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_STOP,
                COMMAND_SEEK_TO_NEXT,
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_TO_PREVIOUS,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                COMMAND_SET_SPEED_AND_PITCH,
                COMMAND_GET_CURRENT_MEDIA_ITEM,
                COMMAND_GET_TIMELINE,
                COMMAND_GET_METADATA
            )
            .build()

        val total = estadoActual.totalOraciones
        val estadoReproduccion = when {
            total == 0 -> STATE_IDLE
            else -> STATE_READY
        }

        val subtitulo = if (total > 0) {
            "Oración ${estadoActual.indiceOracionActual + 1} de $total · ${estadoActual.oracionActualTexto}"
        } else {
            "Listo para leer"
        }

        val metadatos = MediaMetadata.Builder()
            .setTitle(estadoActual.oracionActualTexto.ifBlank { tituloArticulo })
            .setArtist(tituloArticulo)
            .setSubtitle(subtitulo)
            .setDisplayTitle(estadoActual.oracionActualTexto.ifBlank { tituloArticulo })
            .build()

        val elementoMultimedia = MediaItem.Builder()
            .setMediaId("voxaccess_articulo_activo")
            .setMediaMetadata(metadatos)
            .build()

        val datosElemento = MediaItemData.Builder("voxaccess_item_0")
            .setMediaItem(elementoMultimedia)
            .setMediaMetadata(metadatos)
            .setIsSeekable(false)
            .build()

        return State.Builder()
            .setAvailableCommands(comandos)
            .setPlayWhenReady(estadoActual.reproduciendo, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(estadoReproduccion)
            .setPlaybackParameters(PlaybackParameters(estadoActual.velocidad))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setPlaylist(listOf(datosElemento))
            .setCurrentMediaItemIndex(0)
            .setPlaylistMetadata(metadatos)
            .build()
    }

    override fun handleSetPlayWhenReady(reproducirCuandoListo: Boolean): ListenableFuture<*> {
        if (reproducirCuandoListo) {
            controladorTTS.reproducir()
        } else {
            controladorTTS.pausar()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        controladorTTS.pausar()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        indiceElementoMultimedia: Int,
        posicionMilisegundos: Long,
        comandoSalto: Int
    ): ListenableFuture<*> {
        when (comandoSalto) {
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> controladorTTS.siguienteOracion()
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> controladorTTS.anteriorOracion()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(
        parametrosReproduccion: PlaybackParameters
    ): ListenableFuture<*> {
        controladorTTS.cambiarVelocidad(parametrosReproduccion.speed)
        return Futures.immediateVoidFuture()
    }
}
