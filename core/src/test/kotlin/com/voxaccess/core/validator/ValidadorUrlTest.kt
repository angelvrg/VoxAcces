package com.voxaccess.core.validator

import org.junit.Assert.*
import org.junit.Test

class ValidadorUrlTest {

    @Test
    fun testNormalizacionHttpAHttps_CP03() {
        val resultado = ValidadorUrl.validarUrlDirecta("http://es.wikipedia.org/wiki/P%C3%A9njamo")
        assertTrue(resultado is ResultadoValidacionUrl.Valida)
        val valida = resultado as ResultadoValidacionUrl.Valida
        assertEquals("https://es.wikipedia.org/wiki/P%C3%A9njamo", valida.urlNormalizada)
    }

    @Test
    fun testRechazoEsquemaInvalidoOIpPrivada_CP04() {
        // IP privada
        val resIp = ValidadorUrl.validarUrlDirecta("http://192.168.1.1/articulo")
        assertTrue(resIp is ResultadoValidacionUrl.Invalida)

        // Esquema javascript
        val resJs = ValidadorUrl.validarUrlDirecta("javascript:alert(1)")
        assertTrue(resJs is ResultadoValidacionUrl.Invalida)

        // Esquema file
        val resFile = ValidadorUrl.validarUrlDirecta("file:///etc/passwd")
        assertTrue(resFile is ResultadoValidacionUrl.Invalida)

        // Texto sin enlace
        val resTexto = ValidadorUrl.extraerYValidar("solo texto sin enlace")
        assertTrue(resTexto is ResultadoValidacionUrl.Invalida)
    }

    @Test
    fun testExtraccionDesdeTextoCompartido_CP02() {
        val textoCompartido = "Mira este artículo de la BBC: https://www.bbc.com/mundo/noticias-12345 ¡muy interesante!"
        val resultado = ValidadorUrl.extraerYValidar(textoCompartido)
        assertTrue(resultado is ResultadoValidacionUrl.Valida)
        val valida = resultado as ResultadoValidacionUrl.Valida
        assertEquals("https://www.bbc.com/mundo/noticias-12345", valida.urlNormalizada)
    }
}
