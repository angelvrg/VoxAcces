package com.voxaccess.core.segmentation

import java.text.BreakIterator
import java.util.Locale

class SegmentadorOraciones {

    private val ABREVIATURAS = setOf(
        "dr", "dra", "sr", "sra", "srta", "prof", "profa", "ing", "lic",
        "ee", "uu", "ee. uu", "ee.uu", "eu", "etc", "pág", "págs", "pag", "pags",
        "vol", "núm", "num", "no", "art", "cap", "gob", "cia", "ud", "uds",
        "vs", "av", "calle", "tel", "a.m", "p.m", "m.n"
    )

    fun segmentar(texto: String, idioma: String = "es"): List<String> {
        val textoLimpio = texto.trim()
        if (textoLimpio.isEmpty()) return emptyList()

        val locale = if (idioma.startsWith("en", ignoreCase = true)) Locale.ENGLISH else Locale("es", "MX")
        val iterator = BreakIterator.getSentenceInstance(locale)
        iterator.setText(textoLimpio)

        val oracionesCandidatas = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val oracion = textoLimpio.substring(start, end).trim()
            if (oracion.isNotEmpty()) {
                oracionesCandidatas.add(oracion)
            }
            start = end
            end = iterator.next()
        }

        // Unir oraciones cortadas por abreviaturas, iniciales o números
        val oracionesCorregidas = mutableListOf<String>()
        var buffer = ""

        for (i in oracionesCandidatas.indices) {
            val actual = oracionesCandidatas[i]
            val candidata = if (buffer.isEmpty()) actual else "$buffer $actual"

            if (debeFusionarseConSiguiente(actual) && i < oracionesCandidatas.size - 1) {
                buffer = candidata
            } else {
                oracionesCorregidas.add(candidata)
                buffer = ""
            }
        }

        if (buffer.isNotEmpty()) {
            oracionesCorregidas.add(buffer)
        }

        return oracionesCorregidas
    }

    private fun debeFusionarseConSiguiente(oracion: String): Boolean {
        val limpia = oracion.trimEnd()
        if (!limpia.endsWith(".")) return false

        // Inicial suelta con punto, por ejemplo "J." o "A."
        val palabras = limpia.split("\\s+".toRegex())
        val ultimaPalabra = palabras.lastOrNull()?.trimEnd('.') ?: return false

        if (ultimaPalabra.length == 1 && ultimaPalabra[0].isLetter()) {
            return true
        }

        // Abreviatura conocida
        val palabraLower = ultimaPalabra.lowercase()
        if (ABREVIATURAS.contains(palabraLower)) {
            return true
        }

        // Casos como "EE. UU."
        if (palabraLower.endsWith("ee. uu") || palabraLower == "ee.uu") {
            return true
        }

        // Número al final de la oración, por ejemplo "3."
        if (ultimaPalabra.all { it.isDigit() }) {
            return true
        }

        return false
    }
}
