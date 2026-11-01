package com.voxaccess.app

import android.app.Application
import com.voxaccess.app.audio.ControladorTTS
import com.voxaccess.app.preferences.PreferenciasApp
import com.voxaccess.app.service.ServicioLectura
import com.voxaccess.app.util.GestorHaptico
import com.voxaccess.core.repository.RepositorioArticulos
import com.voxaccess.core.storage.AlmacenamientoArticulos

class VoxAccessApp : Application() {

    lateinit var almacenamiento: AlmacenamientoArticulos
        private set

    lateinit var repositorioArticulos: RepositorioArticulos
        private set

    lateinit var preferencias: PreferenciasApp
        private set

    lateinit var gestorHaptico: GestorHaptico
        private set

    lateinit var controladorTTS: ControladorTTS
        private set

    override fun onCreate() {
        super.onCreate()
        instancia = this

        almacenamiento = AlmacenamientoArticulos(filesDir)
        repositorioArticulos = RepositorioArticulos(almacenamiento = almacenamiento)
        preferencias = PreferenciasApp(this)
        gestorHaptico = GestorHaptico(this)

        controladorTTS = ControladorTTS(
            contexto = this,
            alCambiarEstado = { estado ->
                val titulo = repositorioArticulos.obtenerArticuloActivo()?.titulo ?: "VoxAccess"
                if (estado.reproduciendo) {
                    ServicioLectura.iniciarOActualizar(this, titulo, estado)
                } else {
                    ServicioLectura.instanciaActiva?.sincronizarEstado(titulo, estado)
                }
            }
        )
    }

    companion object {
        lateinit var instancia: VoxAccessApp
            private set
    }
}
