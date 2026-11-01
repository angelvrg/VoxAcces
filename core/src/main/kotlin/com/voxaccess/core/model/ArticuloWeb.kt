package com.voxaccess.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class FuenteExtraccion {
    JSON_LD,
    READABILITY,
    COMBINADA,
    FALLBACK
}

/**
 * Contenido extraído y procesado de un artículo web.
 */
@Serializable
data class ArticuloWeb(
    val id: String,
    val urlOriginal: String,
    val urlCanonica: String = urlOriginal,
    val titulo: String,
    val autor: String? = null,
    val fechaPublicacion: String? = null,
    val idioma: String = "es",
    val textoCompleto: String,
    val oraciones: List<Oracion> = emptyList(),
    val fuenteExtraccion: FuenteExtraccion = FuenteExtraccion.COMBINADA,
    val tiempoDescargaMs: Long = 0L,
    val totalPalabras: Int = 0,
    val tieneTablasAccesibles: Boolean = false,
    val fechaGuardadoEpochMs: Long = System.currentTimeMillis()
)
