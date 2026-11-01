package com.voxaccess.core.extractor

import com.voxaccess.core.model.ArticuloWeb
import com.voxaccess.core.model.FuenteExtraccion
import com.voxaccess.core.model.Oracion
import com.voxaccess.core.segmentation.ProcesadorSSML
import org.jsoup.Jsoup
import java.util.UUID

class ExtraccionFallidaException(mensaje: String) : Exception(mensaje)

class ExtractorTresCapas(
    private val extractorJsonLd: ExtractorJsonLd = ExtractorJsonLd(),
    private val conversorTablas: ConversorTablasAccesibles = ConversorTablasAccesibles(),
    private val extractorReadability: ExtractorReadability = ExtractorReadability(),
    private val procesadorSSML: ProcesadorSSML = ProcesadorSSML()
) {

    fun extraerArticulo(
        html: String,
        urlOriginal: String,
        tiempoDescargaMs: Long = 0L
    ): ArticuloWeb {
        val doc = Jsoup.parse(html, urlOriginal)

        // Capa 1: metadatos estructurados JSON-LD
        val datosJsonLd = extractorJsonLd.extraer(doc)

        val idiomaDetectado = datosJsonLd?.idioma
            ?: doc.selectFirst("html")?.attr("lang")?.take(2)?.lowercase()
            ?: doc.selectFirst("meta[http-equiv=content-language]")?.attr("content")?.take(2)?.lowercase()
            ?: "es"

        var titulo = datosJsonLd?.titulo
        val autor = datosJsonLd?.autor
        val fecha = datosJsonLd?.fechaPublicacion

        val todasLasOraciones = mutableListOf<Oracion>()
        var fuente = FuenteExtraccion.COMBINADA
        var textoCompleto = ""
        var huboTablas = false

        // Caso 1: JSON-LD trae el cuerpo completo del artículo
        val bodyJsonLd = datosJsonLd?.cuerpoArticulo
        if (!bodyJsonLd.isNullOrEmpty() && bodyJsonLd.length >= 300) {
            fuente = FuenteExtraccion.JSON_LD
            textoCompleto = bodyJsonLd
            if (titulo.isNullOrBlank()) {
                titulo = doc.title().ifBlank { "Artículo web" }
            }

            var indiceGlobal = 1

            // El título se lee como primera oración
            todasLasOraciones.addAll(
                procesadorSSML.procesarTexto(
                    texto = titulo,
                    idioma = idiomaDetectado,
                    indiceInicial = indiceGlobal,
                    esEncabezado = true
                )
            )
            indiceGlobal += todasLasOraciones.size

            // Segmentar el cuerpo del artículo
            val oracionesCuerpo = procesadorSSML.procesarTexto(
                texto = bodyJsonLd,
                idioma = idiomaDetectado,
                indiceInicial = indiceGlobal
            )
            todasLasOraciones.addAll(oracionesCuerpo)
        } else {
            // Caso 2: Jsoup, conversión de tablas y Readability
            fuente = FuenteExtraccion.READABILITY
            val docParaReadability = doc.clone()

            // Convertir tablas HTML en texto accesible
            val tablasModificadas = conversorTablas.convertirTablasEnDocumento(docParaReadability)
            huboTablas = tablasModificadas > 0

            // Extraer el contenido principal
            val contenidoReadability = extractorReadability.extraer(docParaReadability, urlOriginal)

            if (titulo.isNullOrBlank()) {
                titulo = contenidoReadability.titulo.ifBlank { doc.title().ifBlank { "Artículo web" } }
            }

            var indiceGlobal = 1

            // El título se lee como primera oración
            todasLasOraciones.addAll(
                procesadorSSML.procesarTexto(
                    texto = titulo,
                    idioma = idiomaDetectado,
                    indiceInicial = indiceGlobal,
                    esEncabezado = true
                )
            )
            indiceGlobal += todasLasOraciones.size

            // Procesar párrafos y tablas
            for (parrafo in contenidoReadability.subtitulosYParrafos) {
                val oracionesParrafo = procesadorSSML.procesarTexto(
                    texto = parrafo.texto,
                    idioma = idiomaDetectado,
                    indiceInicial = indiceGlobal,
                    esEncabezado = parrafo.esEncabezado,
                    esTabla = parrafo.esTabla
                )
                todasLasOraciones.addAll(oracionesParrafo)
                indiceGlobal += oracionesParrafo.size
            }

            textoCompleto = contenidoReadability.textoPlano
        }

        val totalPalabras = textoCompleto.split("\\s+".toRegex()).count { it.isNotBlank() }

        // Validar contenido mínimo: portada o página vacía
        if (totalPalabras < 25 && todasLasOraciones.size <= 2) {
            throw ExtraccionFallidaException(
                "No se encontró un artículo legible en este enlace. La página puede requerir JavaScript o ser una portada."
            )
        }

        return ArticuloWeb(
            id = UUID.nameUUIDFromBytes(urlOriginal.toByteArray()).toString(),
            urlOriginal = urlOriginal,
            urlCanonica = doc.selectFirst("link[rel=canonical]")?.attr("href")?.ifBlank { null } ?: urlOriginal,
            titulo = titulo ?: "Artículo web",
            autor = autor,
            fechaPublicacion = fecha,
            idioma = idiomaDetectado,
            textoCompleto = textoCompleto,
            oraciones = todasLasOraciones,
            fuenteExtraccion = fuente,
            tiempoDescargaMs = tiempoDescargaMs,
            totalPalabras = totalPalabras,
            tieneTablasAccesibles = huboTablas
        )
    }
}
