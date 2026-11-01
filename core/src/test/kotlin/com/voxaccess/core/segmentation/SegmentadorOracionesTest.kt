package com.voxaccess.core.segmentation

import org.junit.Assert.*
import org.junit.Test

class SegmentadorOracionesTest {

    private val segmentador = SegmentadorOraciones()
    private val procesadorSSML = ProcesadorSSML(segmentador)

    @Test
    fun testAbreviaturasYDecimalesEnEspanol_CP11() {
        val texto = "El Dr. Pérez y la Sra. Gómez viajaron a EE. UU. en la pág. 14 del informe. La constante pi vale 3.14 aproximadamente."
        val oraciones = segmentador.segmentar(texto, "es")

        assertEquals(2, oraciones.size)
        assertEquals("El Dr. Pérez y la Sra. Gómez viajaron a EE. UU. en la pág. 14 del informe.", oraciones[0])
        assertEquals("La constante pi vale 3.14 aproximadamente.", oraciones[1])
    }

    @Test
    fun testGeneracionSSMLBienFormado() {
        val oraciones = procesadorSSML.procesarTexto(
            texto = "Título del artículo & noticias. Primera oración completa.",
            idioma = "es",
            indiceInicial = 1
        )

        assertEquals(2, oraciones.size)
        assertEquals(1, oraciones[0].indice)
        assertTrue(oraciones[0].marcadoSSML.startsWith("<speak><s>"))
        assertTrue(oraciones[0].marcadoSSML.endsWith("</speak>"))
        assertTrue(oraciones[0].marcadoSSML.contains("&amp;")) // Escapado de XML
    }
}
