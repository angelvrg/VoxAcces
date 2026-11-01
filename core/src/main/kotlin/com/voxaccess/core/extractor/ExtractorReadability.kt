package com.voxaccess.core.extractor

import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

data class ContenidoExtraido(
    val titulo: String,
    val subtitulosYParrafos: List<ParrafoArticulo>,
    val textoPlano: String
)

data class ParrafoArticulo(
    val texto: String,
    val esEncabezado: Boolean = false,
    val esTabla: Boolean = false
)

class ExtractorReadability {

    private val selectoresRuido = listOf(
        "nav", "header", "footer", "aside", "form", "iframe",
        ".cookie", ".cookies", "#cookie-banner", ".ad", ".ads",
        ".advertisement", ".banner-ad", ".social-share", ".share-buttons",
        ".mw-editsection", ".reference", ".reflist", ".navbox",
        ".sidebar", ".comments", "#comments", ".related-posts",
        ".te-recomendamos", ".en-xataka"
    )

    fun extraer(docClonado: Document, url: String): ContenidoExtraido {
        // Limpieza inicial del DOM
        limpiarElementosDeRuido(docClonado)

        // Título prioritario
        val titulo = extraerTitulo(docClonado)

        // Extracción con Readability4J
        var textoFinal = ""
        val parrafos = mutableListOf<ParrafoArticulo>()

        try {
            val readability = Readability4J(url, docClonado.html())
            val article = readability.parse()

            val contentHtml = article.content
                ?: article.articleContent?.html()

            if (!contentHtml.isNullOrEmpty()) {
                val docArticulo = Jsoup.parse(contentHtml)
                procesarNodosContenido(docArticulo.body(), parrafos)
            }
        } catch (_: Exception) {
            // Documento atípico: se usa la extracción directa
        }

        // Si hay poco texto, extraer directamente de <article> o <main>
        if (parrafos.sumOf { it.texto.length } < 150) {
            parrafos.clear()
            val contenedor = docClonado.selectFirst("article, [role=main], main, .article-body, .entry-content")
                ?: docClonado.body()
            procesarNodosContenido(contenedor, parrafos)
        }

        textoFinal = parrafos.joinToString("\n\n") { it.texto }

        return ContenidoExtraido(
            titulo = titulo,
            subtitulosYParrafos = parrafos,
            textoPlano = textoFinal
        )
    }

    private fun limpiarElementosDeRuido(doc: Document) {
        doc.select("script, style, noscript").remove()

        for (selector in selectoresRuido) {
            doc.select(selector).remove()
        }

        // Quitar referencias tipo [1], [2] o [editar] de Wikipedia
        doc.select(".mw-editsection, sup.reference").remove()
    }

    private fun extraerTitulo(doc: Document): String {
        return doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
            ?.ifEmpty { null }
            ?: doc.selectFirst("h1")?.text()?.trim()?.ifEmpty { null }
            ?: doc.title().trim().substringBefore(" - ").substringBefore(" | ")
            ?: "Artículo sin título"
    }

    private fun procesarNodosContenido(contenedor: Element, parrafos: MutableList<ParrafoArticulo>) {
        val elementos = contenedor.select("h1, h2, h3, h4, p, li, .vox-tabla-accesible")

        for (elem in elementos) {
            val texto = limpiarTextoParrafo(elem.text())
            if (texto.length < 5) continue

            // Descartar textos de ruido que quedan en párrafos
            if (esTextoDeRuido(texto)) continue

            val esEncabezado = elem.tagName().startsWith("h")
            val esTabla = elem.hasClass("vox-tabla-accesible")

            parrafos.add(
                ParrafoArticulo(
                    texto = texto,
                    esEncabezado = esEncabezado,
                    esTabla = esTabla
                )
            )
        }
    }

    private fun limpiarTextoParrafo(texto: String): String {
        return texto
            .replace("\\[\\d+\\]".toRegex(), "") // Referencias [1], [23]
            .replace("\\[editar\\]".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun esTextoDeRuido(texto: String): Boolean {
        val lower = texto.lowercase()
        return lower.startsWith("fuente de la imagen") ||
                lower.startsWith("pie de foto") ||
                lower.startsWith("lee también:") ||
                lower.startsWith("en xataka |") ||
                lower.startsWith("te recomendamos:") ||
                lower.startsWith("suscríbete a") ||
                lower.startsWith("copyright ©")
    }
}
