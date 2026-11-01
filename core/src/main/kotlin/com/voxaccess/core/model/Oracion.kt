package com.voxaccess.core.model

import kotlinx.serialization.Serializable

/**
 * Oración segmentada lista para la síntesis de voz.
 *
 * @property indice Posición de la oración dentro del artículo.
 * @property textoPlano Texto normalizado, sin etiquetas.
 * @property marcadoSSML Texto con etiquetas SSML 1.1.
 * @property duracionEstimadaMs Tiempo estimado de lectura a velocidad 1.0x.
 * @property esEncabezado Indica si proviene de un título o subtítulo.
 * @property esElementoTabla Indica si proviene de una tabla HTML.
 */
@Serializable
data class Oracion(
    val indice: Int,
    val textoPlano: String,
    val marcadoSSML: String,
    val duracionEstimadaMs: Long = 0L,
    val esEncabezado: Boolean = false,
    val esElementoTabla: Boolean = false
)
