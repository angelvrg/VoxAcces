package com.voxaccess.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.voxaccess.app.MainActivity
import com.voxaccess.app.VoxAccessApp
import com.voxaccess.app.audio.EstadoReproductor
import com.voxaccess.app.audio.TtsMediaPlayer

@OptIn(UnstableApi::class)
class ServicioLectura : MediaSessionService() {

    private var sesionMultimedia: MediaSession? = null
    private var reproductorTts: TtsMediaPlayer? = null

    companion object {
        const val CANAL_ID = "voxaccess_lectura_canal"
        const val NOTIFICACION_ID = 1001

        const val ACCION_PLAY_PAUSE = "com.voxaccess.app.ACCION_PLAY_PAUSE"
        const val ACCION_SIGUIENTE = "com.voxaccess.app.ACCION_SIGUIENTE"
        const val ACCION_ANTERIOR = "com.voxaccess.app.ACCION_ANTERIOR"

        var instanciaActiva: ServicioLectura? = null
            private set

        fun iniciarOActualizar(
            contexto: Context,
            tituloArticulo: String,
            estado: EstadoReproductor
        ) {
            val instancia = instanciaActiva
            if (instancia != null) {
                instancia.sincronizarEstado(tituloArticulo, estado)
                return
            }

            try {
                val intencion = Intent(contexto, ServicioLectura::class.java)
                ContextCompat.startForegroundService(contexto, intencion)
            } catch (_: Exception) {
                // Si el sistema restringe iniciar el servicio en segundo plano,
                // la lectura por voz continúa de todos modos.
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instanciaActiva = this
        crearCanalNotificacion()

        val aplicacion = application as VoxAccessApp
        val reproductor = TtsMediaPlayer(aplicacion.controladorTTS).also {
            val titulo = aplicacion.repositorioArticulos.obtenerArticuloActivo()?.titulo ?: "VoxAccess"
            it.fijarTituloArticulo(titulo)
        }
        reproductorTts = reproductor

        val intencionAbrir = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val intencionPendienteAbrir = PendingIntent.getActivity(
            this,
            0,
            intencionAbrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        sesionMultimedia = MediaSession.Builder(this, reproductor)
            .setSessionActivity(intencionPendienteAbrir)
            .build()
    }

    override fun onStartCommand(intencion: Intent?, banderas: Int, idInicio: Int): Int {
        super.onStartCommand(intencion, banderas, idInicio)

        val aplicacion = application as VoxAccessApp
        val controlador = aplicacion.controladorTTS

        when (intencion?.action) {
            ACCION_PLAY_PAUSE -> controlador.alternarPausaReanudar()
            ACCION_SIGUIENTE -> controlador.siguienteOracion()
            ACCION_ANTERIOR -> controlador.anteriorOracion()
        }

        val titulo = aplicacion.repositorioArticulos.obtenerArticuloActivo()?.titulo ?: "VoxAccess"
        sincronizarEstado(titulo, controlador.estado.value)

        return START_STICKY
    }

    override fun onTaskRemoved(intencionRaiz: Intent?) {
        // Mantener la reproducción activa en segundo plano hasta que el usuario
        // pause o detenga desde la notificación o auriculares, o termine el artículo.
        val aplicacion = application as VoxAccessApp
        if (!aplicacion.controladorTTS.estado.value.reproduciendo) {
            stopSelf()
        }
    }

    override fun onGetSession(informacionControlador: MediaSession.ControllerInfo): MediaSession? {
        return sesionMultimedia
    }

    fun sincronizarEstado(tituloArticulo: String, estado: EstadoReproductor) {
        reproductorTts?.fijarTituloArticulo(tituloArticulo)
        mostrarNotificacion(
            tituloArticulo = tituloArticulo,
            oracionActual = estado.oracionActualTexto,
            reproduciendo = estado.reproduciendo
        )
    }

    fun mostrarNotificacion(tituloArticulo: String, oracionActual: String, reproduciendo: Boolean) {
        val sesion = sesionMultimedia ?: return

        val intencionAbrir = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val intencionPendienteAbrir = PendingIntent.getActivity(
            this,
            0,
            intencionAbrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accionAnterior = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Oración anterior",
            crearPendingIntentAccion(ACCION_ANTERIOR, 1)
        )
        val accionReproducirPausar = NotificationCompat.Action(
            if (reproduciendo) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (reproduciendo) "Pausar lectura" else "Reanudar lectura",
            crearPendingIntentAccion(ACCION_PLAY_PAUSE, 2)
        )
        val accionSiguiente = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Oración siguiente",
            crearPendingIntentAccion(ACCION_SIGUIENTE, 3)
        )

        val constructorNotificacion = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(tituloArticulo.ifEmpty { "VoxAccess" })
            .setContentText(oracionActual.ifEmpty { "Lectura activa" })
            .setContentIntent(intencionPendienteAbrir)
            .setOngoing(reproduciendo)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(accionAnterior)
            .addAction(accionReproducirPausar)
            .addAction(accionSiguiente)
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(sesion)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        val notificacion: Notification = constructorNotificacion.build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICACION_ID,
                    notificacion,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICACION_ID, notificacion)
            }
        } catch (_: Exception) {
            val gestorNotificaciones = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            gestorNotificaciones.notify(NOTIFICACION_ID, notificacion)
        }
    }

    private fun crearPendingIntentAccion(accion: String, codigoSolicitud: Int): PendingIntent {
        val intencion = Intent(this, ServicioLectura::class.java).apply {
            action = accion
        }
        return PendingIntent.getService(
            this,
            codigoSolicitud,
            intencion,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Reproducción de Artículos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de lectura accesible en segundo plano y pantalla bloqueada"
            }
            val gestorNotificaciones = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            gestorNotificaciones.createNotificationChannel(canal)
        }
    }

    override fun onDestroy() {
        sesionMultimedia?.run {
            release()
            sesionMultimedia = null
        }
        reproductorTts = null
        instanciaActiva = null
        super.onDestroy()
    }
}
