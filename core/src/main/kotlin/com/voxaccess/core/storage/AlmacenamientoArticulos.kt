package com.voxaccess.core.storage

import com.voxaccess.core.model.ArticuloWeb
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AlmacenamientoArticulos(
    directorioBase: File,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
) {

    private val carpetaArticulos = File(directorioBase, "articulos_json").apply { mkdirs() }
    private val carpetaHtml = File(directorioBase, "articulos_html").apply { mkdirs() }
    private val archivoUltimoId = File(directorioBase, "ultimo_articulo_id.txt")

    @Synchronized
    fun guardar(articulo: ArticuloWeb, htmlOriginal: String? = null) {
        val archivoJson = File(carpetaArticulos, "${articulo.id}.json")
        archivoJson.writeText(json.encodeToString(articulo), Charsets.UTF_8)

        if (!htmlOriginal.isNullOrEmpty()) {
            val archivoHtml = File(carpetaHtml, "${articulo.id}.html")
            archivoHtml.writeText(htmlOriginal, Charsets.UTF_8)
        }

        archivoUltimoId.writeText(articulo.id, Charsets.UTF_8)
    }

    @Synchronized
    fun obtenerUltimo(): ArticuloWeb? {
        if (!archivoUltimoId.exists()) return null
        val id = archivoUltimoId.readText().trim()
        if (id.isEmpty()) return null
        return obtenerPorId(id)
    }

    @Synchronized
    fun obtenerPorId(id: String): ArticuloWeb? {
        val archivo = File(carpetaArticulos, "$id.json")
        if (!archivo.exists()) return null
        return try {
            json.decodeFromString<ArticuloWeb>(archivo.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun obtenerHtmlOriginal(id: String): String? {
        val archivo = File(carpetaHtml, "$id.html")
        if (!archivo.exists()) return null
        return archivo.readText(Charsets.UTF_8)
    }

    @Synchronized
    fun listarTodos(): List<ArticuloWeb> {
        val archivos = carpetaArticulos.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return archivos.mapNotNull { archivo ->
            try {
                json.decodeFromString<ArticuloWeb>(archivo.readText(Charsets.UTF_8))
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.fechaGuardadoEpochMs }
    }

    @Synchronized
    fun eliminar(id: String): Boolean {
        val archivoJson = File(carpetaArticulos, "$id.json")
        val archivoHtml = File(carpetaHtml, "$id.html")
        val eliminado = archivoJson.delete()
        archivoHtml.delete()
        return eliminado
    }
}
