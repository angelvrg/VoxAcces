package com.voxaccess.core.segmentation

import com.voxaccess.core.model.Oracion

class ProcesadorSSML(
    private val segmentador: SegmentadorOraciones = SegmentadorOraciones()
) {

    /**
     * Convierte un texto en una lista de oraciones con SSML.
     */
    fun procesarTexto(
        texto: String,
        idioma: String = "es",
        indiceInicial: Int = 1,
        esEncabezado: Boolean = false,
        esTabla: Boolean = false
    ): List<Oracion> {
        val cadenas = segmentador.segmentar(texto, idioma)
        return cadenas.mapIndexed { idx, str ->
            val indice = indiceInicial + idx
            val ssml = generarSSML(str, esEncabezado)
            val duracion = estimarDuracionMs(str)

            Oracion(
                indice = indice,
                textoPlano = str,
                marcadoSSML = ssml,
                duracionEstimadaMs = duracion,
                esEncabezado = esEncabezado,
                esElementoTabla = esTabla
            )
        }
    }

    /**
     * Genera el marcado SSML 1.1 de una oración.
     */
    fun generarSSML(texto: String, esEncabezado: Boolean = false): String {
        val escapado = escaparXml(texto)
        val pausa = if (esEncabezado) "400ms" else "250ms"
        return "<speak><s>$escapado</s><break time=\"$pausa\"/></speak>"
    }

    /**
     * Estima la duración de la oración a velocidad 1.0x.
     */
    fun estimarDuracionMs(texto: String): Long {
        val palabras = texto.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        val tiempoPalabras = palabras * 380L
        return (tiempoPalabras + 300L).coerceAtLeast(600L)
    }

    private fun escaparXml(texto: String): String {
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
