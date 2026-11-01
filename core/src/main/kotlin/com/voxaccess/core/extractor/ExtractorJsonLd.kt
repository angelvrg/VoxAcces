package com.voxaccess.core.extractor

import kotlinx.serialization.json.*
import org.jsoup.nodes.Document

data class DatosJsonLd(
    val titulo: String? = null,
    val autor: String? = null,
    val fechaPublicacion: String? = null,
    val idioma: String? = null,
    val cuerpoArticulo: String? = null
)

class ExtractorJsonLd(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {

    private val TIPOS_ARTICULO = setOf(
        "newsarticle",
        "article",
        "blogposting",
        "report",
        "techarticle",
        "scholarlyarticle",
        "medicalwebpage"
    )

    fun extraer(doc: Document): DatosJsonLd? {
        val scripts = doc.select("script[type=application/ld+json]")
        if (scripts.isEmpty()) return null

        var mejorResultado: DatosJsonLd? = null

        for (script in scripts) {
            val contenido = script.data().trim()
            if (contenido.isEmpty()) continue

            try {
                val elemento = json.parseToJsonElement(contenido)
                val resultado = buscarEnElemento(elemento)
                if (resultado != null) {
                    // Priorizar el bloque con un cuerpo de artículo completo
                    if (resultado.cuerpoArticulo != null && resultado.cuerpoArticulo.length >= 300) {
                        return resultado
                    }
                    if (mejorResultado == null) {
                        mejorResultado = resultado
                    }
                }
            } catch (_: Exception) {
                // Ignorar bloques JSON-LD malformados
            }
        }

        return mejorResultado
    }

    private fun buscarEnElemento(elem: JsonElement): DatosJsonLd? {
        return when (elem) {
            is JsonObject -> procesarObjeto(elem)
            is JsonArray -> {
                for (item in elem) {
                    val res = buscarEnElemento(item)
                    if (res?.cuerpoArticulo != null && res.cuerpoArticulo.length >= 300) {
                        return res
                    }
                }
                null
            }
            else -> null
        }
    }

    private fun procesarObjeto(obj: JsonObject): DatosJsonLd? {
        // Recorrer la estructura anidada @graph
        if (obj.containsKey("@graph") && obj["@graph"] is JsonArray) {
            val graphArray = obj["@graph"]!!.jsonArray
            for (nodo in graphArray) {
                val res = buscarEnElemento(nodo)
                if (res != null) return res
            }
        }

        val tipo = obtenerTipo(obj)
        if (tipo == null || !TIPOS_ARTICULO.contains(tipo.lowercase())) {
            return null
        }

        val titulo = obj["headline"]?.jsonPrimitive?.contentOrNull
            ?: obj["name"]?.jsonPrimitive?.contentOrNull

        val autor = extraerAutor(obj["author"])
        val fecha = obj["datePublished"]?.jsonPrimitive?.contentOrNull
        val idioma = obj["inLanguage"]?.jsonPrimitive?.contentOrNull
        val cuerpo = obj["articleBody"]?.jsonPrimitive?.contentOrNull

        return DatosJsonLd(
            titulo = titulo?.trim(),
            autor = autor?.trim(),
            fechaPublicacion = fecha?.trim(),
            idioma = normalizarIdioma(idioma),
            cuerpoArticulo = cuerpo?.trim()
        )
    }

    private fun obtenerTipo(obj: JsonObject): String? {
        val tipoElem = obj["@type"] ?: return null
        return when (tipoElem) {
            is JsonPrimitive -> tipoElem.contentOrNull
            is JsonArray -> tipoElem.mapNotNull { it.jsonPrimitive.contentOrNull }.firstOrNull {
                TIPOS_ARTICULO.contains(it.lowercase())
            }
            else -> null
        }
    }

    private fun extraerAutor(autorElem: JsonElement?): String? {
        if (autorElem == null) return null
        return when (autorElem) {
            is JsonPrimitive -> autorElem.contentOrNull
            is JsonObject -> autorElem["name"]?.jsonPrimitive?.contentOrNull
            is JsonArray -> {
                autorElem.mapNotNull { item ->
                    when (item) {
                        is JsonPrimitive -> item.contentOrNull
                        is JsonObject -> item["name"]?.jsonPrimitive?.contentOrNull
                        else -> null
                    }
                }.joinToString(", ").ifEmpty { null }
            }
            else -> null
        }
    }

    private fun normalizarIdioma(idiomaStr: String?): String? {
        if (idiomaStr == null) return null
        val codigo = idiomaStr.trim().lowercase().split("-", "_").firstOrNull() ?: "es"
        return if (codigo.length == 2) codigo else "es"
    }
}
