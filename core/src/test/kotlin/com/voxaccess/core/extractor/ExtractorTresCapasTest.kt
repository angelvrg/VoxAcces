package com.voxaccess.core.extractor

import com.voxaccess.core.model.FuenteExtraccion
import org.junit.Assert.*
import org.junit.Test

class ExtractorTresCapasTest {

    private val extractor = ExtractorTresCapas()

    @Test
    fun testExtraccionCapa1JsonLd_CP06() {
        val cuerpoEsperado = "La inteligencia artificial está transformando el acceso a la información para personas con discapacidad visual en todo el mundo. Nuevos modelos locales permiten procesar y sintetizar voz en dispositivos móviles sin necesidad de transmitir datos a servidores externos, garantizando la privacidad de los usuarios y una respuesta inmediata."
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <title>Título de la página</title>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "NewsArticle",
                    "headline": "Avances en accesibilidad digital",
                    "author": { "@type": "Person", "name": "Valentin Gomez" },
                    "datePublished": "2026-10-31",
                    "inLanguage": "es",
                    "articleBody": "$cuerpoEsperado"
                }
                </script>
            </head>
            <body>
                <nav><a href="/">Inicio</a></nav>
                <div>Menú y publicidad</div>
            </body>
            </html>
        """.trimIndent()

        val articulo = extractor.extraerArticulo(html, "https://noticias.example.com/accesibilidad")

        assertEquals(FuenteExtraccion.JSON_LD, articulo.fuenteExtraccion)
        assertEquals("Avances en accesibilidad digital", articulo.titulo)
        assertEquals("Valentin Gomez", articulo.autor)
        assertEquals("es", articulo.idioma)
        assertTrue(articulo.oraciones.isNotEmpty())
    }

    @Test
    fun testExtraccionCapa2Readability_CP07() {
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <title>Descubrimiento astronómico importante</title>
            </head>
            <body>
                <header><nav>Menú de navegación y enlaces irrelevantes</nav></header>
                <main>
                    <article>
                        <h1>Descubrimiento astronómico importante</h1>
                        <p>Un equipo internacional de astrónomos ha detectado una nueva señal proveniente del centro de la galaxia.</p>
                        <p>Los datos recopilados durante tres meses sugieren que se trata de un pulsar de milisegundos con características únicas.</p>
                        <p>Este hallazgo permitirá contrastar las teorías de la relatividad general en condiciones de gravedad extrema.</p>
                    </article>
                </main>
                <aside><div class="ad">Publicidad molesta</div></aside>
                <footer>Pie de página con derechos reservados</footer>
            </body>
            </html>
        """.trimIndent()

        val articulo = extractor.extraerArticulo(html, "https://ciencia.example.com/pulsar")

        assertEquals(FuenteExtraccion.READABILITY, articulo.fuenteExtraccion)
        assertTrue(articulo.titulo.contains("Descubrimiento astronómico"))
        assertTrue(articulo.oraciones.size >= 3)
        assertFalse(articulo.textoCompleto.contains("Publicidad molesta"))
        assertFalse(articulo.textoCompleto.contains("Menú de navegación"))
    }

    @Test(expected = ExtraccionFallidaException::class)
    fun testDeteccionPaginaVacia_CP20() {
        val html = """
            <html><body><div>Sólo tres palabras</div></body></html>
        """.trimIndent()

        extractor.extraerArticulo(html, "https://ejemplo.com/vacio")
    }
}
