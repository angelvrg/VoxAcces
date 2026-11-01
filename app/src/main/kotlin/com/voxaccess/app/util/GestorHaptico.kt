package com.voxaccess.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class GestorHaptico(contexto: Context) {

    private val vibrador: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val gestorVibrador = contexto.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        gestorVibrador?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        contexto.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var habilitado: Boolean = true

    fun vibrarToqueLigero() {
        if (!habilitado || vibrador == null || !vibrador.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrador.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(35)
        }
    }

    fun vibrarCambioOracion() {
        if (!habilitado || vibrador == null || !vibrador.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrador.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(20)
        }
    }

    fun vibrarConfirmacionFuerte() {
        if (!habilitado || vibrador == null || !vibrador.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrador.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(70)
        }
    }
}
