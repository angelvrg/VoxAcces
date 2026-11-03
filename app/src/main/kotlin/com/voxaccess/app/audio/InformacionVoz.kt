package com.voxaccess.app.audio

/**
 * Representación accesible y amigable de una voz del motor TTS de Android.
 *
 * @property id Identificador técnico interno de la voz en Android (ej. "es-es-x-eed-local").
 * @property nombreAmigable Nombre legible y accesible para la interfaz y TalkBack (ej. "Español (México) · Voz 1").
 * @property idiomaPais Nombre legible del idioma y región (ej. "Español (México)").
 * @property esNatural Indica si la voz es de alta calidad según el motor TTS.
 * @property esPredeterminada Indica si es la voz predeterminada del motor en el dispositivo.
 */
data class InformacionVoz(
    val id: String,
    val nombreAmigable: String,
    val idiomaPais: String,
    val esNatural: Boolean,
    val esPredeterminada: Boolean
)
