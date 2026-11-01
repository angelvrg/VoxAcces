package com.voxaccess.app.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.almacenPreferencias by preferencesDataStore(name = "voxaccess_prefs")

class PreferenciasApp(private val contexto: Context) {

    companion object {
        val CLAVE_VELOCIDAD = floatPreferencesKey("velocidad_inicial")
        val CLAVE_HAPTICO = booleanPreferencesKey("confirmacion_haptica")
        val CLAVE_VOZ_NATURAL = booleanPreferencesKey("voz_natural_activada")
        val CLAVE_NOMBRE_VOZ = stringPreferencesKey("nombre_voz_seleccionada")
    }

    val flujoVelocidadInicial: Flow<Float> = contexto.almacenPreferencias.data.map { preferencias ->
        preferencias[CLAVE_VELOCIDAD] ?: 1.5f
    }

    val flujoHapticoActivo: Flow<Boolean> = contexto.almacenPreferencias.data.map { preferencias ->
        preferencias[CLAVE_HAPTICO] ?: true
    }

    val flujoVozNaturalActiva: Flow<Boolean> = contexto.almacenPreferencias.data.map { preferencias ->
        preferencias[CLAVE_VOZ_NATURAL] ?: true
    }

    val flujoVozSeleccionada: Flow<String> = contexto.almacenPreferencias.data.map { preferencias ->
        preferencias[CLAVE_NOMBRE_VOZ] ?: "Lucía · Español"
    }

    suspend fun guardarVelocidad(velocidad: Float) {
        contexto.almacenPreferencias.edit { preferencias ->
            preferencias[CLAVE_VELOCIDAD] = velocidad.coerceIn(0.75f, 2.5f)
        }
    }

    suspend fun guardarHapticoActivo(activo: Boolean) {
        contexto.almacenPreferencias.edit { preferencias ->
            preferencias[CLAVE_HAPTICO] = activo
        }
    }

    suspend fun guardarVozNaturalActiva(activa: Boolean) {
        contexto.almacenPreferencias.edit { preferencias ->
            preferencias[CLAVE_VOZ_NATURAL] = activa
        }
    }

    suspend fun guardarVozSeleccionada(nombreVoz: String) {
        contexto.almacenPreferencias.edit { preferencias ->
            preferencias[CLAVE_NOMBRE_VOZ] = nombreVoz
        }
    }
}
