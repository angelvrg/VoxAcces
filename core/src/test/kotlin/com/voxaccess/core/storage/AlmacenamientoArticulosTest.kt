package com.voxaccess.core.storage

import com.voxaccess.core.model.ArticuloWeb
import com.voxaccess.core.model.FuenteExtraccion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AlmacenamientoArticulosTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testGuardadoYRecuperacionArticulo() {
        val almacenamiento = AlmacenamientoArticulos(tempFolder.root)

        val articulo = ArticuloWeb(
            id = "test-123",
            urlOriginal = "https://example.com/noticia",
            titulo = "Noticia de prueba",
            textoCompleto = "Contenido de prueba para el almacenamiento persistente.",
            fuenteExtraccion = FuenteExtraccion.JSON_LD
        )

        almacenamiento.guardar(articulo, "<html><body>HTML Original</body></html>")

        val recuperado = almacenamiento.obtenerPorId("test-123")
        assertNotNull(recuperado)
        assertEquals("Noticia de prueba", recuperado!!.titulo)
        assertEquals(articulo.urlOriginal, recuperado.urlOriginal)

        val ultimo = almacenamiento.obtenerUltimo()
        assertNotNull(ultimo)
        assertEquals("test-123", ultimo!!.id)

        val html = almacenamiento.obtenerHtmlOriginal("test-123")
        assertEquals("<html><body>HTML Original</body></html>", html)

        val lista = almacenamiento.listarTodos()
        assertEquals(1, lista.size)

        almacenamiento.eliminar("test-123")
        assertNull(almacenamiento.obtenerPorId("test-123"))
    }
}
