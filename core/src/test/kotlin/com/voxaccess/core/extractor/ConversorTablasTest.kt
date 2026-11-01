package com.voxaccess.core.extractor

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class ConversorTablasTest {

    private val conversor = ConversorTablasAccesibles()

    @Test
    fun testConversionTablaSecuencial_CP08() {
        val html = """
            <html>
            <body>
                <table>
                    <caption>Población de municipios</caption>
                    <thead>
                        <tr><th>Municipio</th><th>Población</th><th>Altitud</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>Pénjamo</td><td>43,249</td><td>1,700 m</td></tr>
                        <tr><td>Irapuato</td><td>592,953</td><td>1,724 m</td></tr>
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val convertidas = conversor.convertirTablasEnDocumento(doc)

        assertEquals(1, convertidas)
        val p = doc.selectFirst("p.vox-tabla-accesible")
        assertNotNull(p)

        val texto = p!!.text()
        assertTrue(texto.contains("Tabla: Población de municipios"))
        assertTrue(texto.contains("Fila 1: Municipio: Pénjamo; Población: 43,249; Altitud: 1,700 m"))
        assertTrue(texto.contains("Fila 2: Municipio: Irapuato; Población: 592,953; Altitud: 1,724 m"))
    }
}
