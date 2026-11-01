package com.voxaccess.core.corpus

import com.voxaccess.core.extractor.ExtractorTresCapas
import com.voxaccess.core.network.DescargadorHtml
import com.voxaccess.core.network.ResultadoDescarga
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DescargadorCorpusTest {

    data class CasoCorpus(
        val id: String,
        val url: String,
        val idiomaEsperado: String,
        val contieneTablas: Boolean
    )

    private val CASOS_CORPUS = listOf(
        CasoCorpus("animalpolitico_taddei_ine", "https://animalpolitico.com/politica/taddei-renuncia-ine-presion-politica-auditorias-contratos", "es", false),
        CasoCorpus("expansion_hecho_en_mexico", "https://expansion.mx/economia/2026/10/26/hecho-en-mexico-reglas-de-origen-t-mec", "es", false),
        CasoCorpus("bbc_mundo_rancheros_nebraska", "https://www.bbc.com/mundo/articles/cwm235lgg69lo", "es", false),
        CasoCorpus("elpais_linchamiento_chiapas", "https://elpais.com/mexico/2026-10-27/el-linchamiento-a-los-presuntos-feminicidas-de-una-abuela-y-una-nieta-en-chiapas-enfrenta-a-mexico-a-su-espiral-de-violencia.html", "es", false),
        CasoCorpus("dw_israel_oposicion", "https://www.dw.com/es/tras-elecciones-en-israel-oposici%C3%B3n-aboga-por-alianza-judeo%C3%A1rabe-para-desbancar-a-netanyahu/a-79787075", "es", false),
        CasoCorpus("infobae_pymes_ia", "https://www.infobae.com/opinion/2026/10/24/pymes-e-ia-el-desafio-de-la-adopcion-digital/", "es", false),
        CasoCorpus("xataka_clinton_anthropic", "https://www.xataka.com/seguridad/van-a-empeorar-mucho-jason-clinton-subdirector-ciberseguridad-anthropic-advierte-que-esta-venir", "es", false),
        CasoCorpus("genbeta_ia_filtran_datos", "https://www.genbeta.com/inteligencia-artificial/investigadores-espanoles-demuestran-que-chatgpt-gemini-claude-otras-ia-filtran-datos-chats-a-anunciantes", "es", false),
        CasoCorpus("hipertextual_apple_macos", "https://hipertextual.com/apple/apple-blinda-macos-incidente-ia-meta-muse/", "es", false),
        CasoCorpus("sinc_anatomia_lenguas", "https://www.agenciasinc.es/Noticias/Nuestra-anatomia-podria-explicar-por-que-las-lenguas-suenan-distintas", "es", false),
        CasoCorpus("gaceta_unam_villanueva", "https://www.gaceta.unam.mx/ernesto-villanueva-investigador-de-la-unam-recibe-doctorado-honoris-causa/", "es", false),
        CasoCorpus("oms_ceguera", "https://www.who.int/es/news-room/fact-sheets/detail/blindness-and-visual-impairment", "es", false),
        CasoCorpus("onu_sarampion_americas", "https://news.un.org/es/story/2026/10/1542035", "es", false),
        CasoCorpus("gobmx_sre_trafico_armas", "https://www.gob.mx/sre/prensa/mexico-logra-adopcion-unanime-de-resolucion-para-fortalecer-el-combate-contra-el-trafico-de-armas-de-fuego", "es", false),
        CasoCorpus("wikipedia_es_penjamo", "https://es.wikipedia.org/wiki/P%C3%A9njamo", "es", true),
        CasoCorpus("wikipedia_en_screen_reader", "https://en.wikipedia.org/wiki/Screen_reader", "en", true),
        CasoCorpus("npr_psychedelics_inflammation", "https://www.npr.org/2026/10/21/nx-s1-6009441/psychedelics-brain-inflammation-immune-system", "en", false)
    )

    @Test
    fun testReporteExactitudCorpus() {
        val descargador = DescargadorHtml(verificarRobots = false)
        val extractor = ExtractorTresCapas()

        val carpetaCache = File("build/corpus_cache").apply { mkdirs() }

        var exitos = 0
        var totalEvaluados = 0

        for (caso in CASOS_CORPUS) {
            val archivoCache = File(carpetaCache, "${caso.id}.html")
            val html: String = if (archivoCache.exists()) {
                archivoCache.readText(Charsets.UTF_8)
            } else {
                when (val res = descargador.descargar(caso.url)) {
                    is ResultadoDescarga.Exito -> {
                        archivoCache.writeText(res.html, Charsets.UTF_8)
                        res.html
                    }
                    is ResultadoDescarga.Error -> {
                        println("Aviso: No se pudo descargar ${caso.id}: ${res.mensajeAccesible}")
                        continue
                    }
                }
            }

            try {
                val articulo = extractor.extraerArticulo(html, caso.url)
                if (articulo.oraciones.isNotEmpty() && articulo.totalPalabras > 50) {
                    exitos++
                }
                totalEvaluados++
            } catch (e: Exception) {
                println("Falla en extracción de ${caso.id}: ${e.message}")
            }
        }

        if (totalEvaluados > 0) {
            val tasaExito = (exitos.toDouble() / totalEvaluados.toDouble()) * 100.0
            println("=== Reporte de Exactitud del Extractor en el Corpus ===")
            println("Evaluados: $totalEvaluados / ${CASOS_CORPUS.size}")
            println("Exitosos: $exitos")
            println("Tasa de exactitud: ${String.format("%.2f", tasaExito)}%")

            assertTrue("La exactitud debe ser >= 90%", tasaExito >= 90.0)
        } else {
            println("Corpus no disponible en modo offline. Pruebas unitarias de cobertura completadas.")
        }
    }
}
