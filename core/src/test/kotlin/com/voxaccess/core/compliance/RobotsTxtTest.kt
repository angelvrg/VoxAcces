package com.voxaccess.core.compliance

import org.junit.Assert.*
import org.junit.Test

class RobotsTxtTest {

    @Test
    fun testReglasAllowYDisallow_CP05() {
        val contenidoRobots = """
            User-agent: *
            Disallow: /privado/
            Disallow: /admin/
            Allow: /privado/publico.html
            
            User-agent: VoxAccess
            Disallow: /no-voxaccess/
        """.trimIndent()

        val robots = RobotsTxt.parsear(contenidoRobots)

        // Ruta permitida
        assertTrue(robots.esRutaPermitida("/articulos/noticia-1"))

        // Ruta prohibida
        assertFalse(robots.esRutaPermitida("/privado/secreto.html"))

        // Excepción más específica
        assertTrue(robots.esRutaPermitida("/privado/publico.html"))

        // Ruta prohibida para VoxAccess
        assertFalse(robots.esRutaPermitida("/no-voxaccess/articulo", agente = "VoxAccess"))
    }
}
