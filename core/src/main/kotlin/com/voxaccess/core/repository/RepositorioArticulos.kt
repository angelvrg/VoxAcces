package com.voxaccess.core.repository

import com.voxaccess.core.extractor.ExtractorTresCapas
import com.voxaccess.core.model.ArticuloWeb
import com.voxaccess.core.network.DescargadorHtml
import com.voxaccess.core.network.ResultadoDescarga
import com.voxaccess.core.storage.AlmacenamientoArticulos
import com.voxaccess.core.validator.ResultadoValidacionUrl
import com.voxaccess.core.validator.ValidadorUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepositorioArticulos(
    private val descargador: DescargadorHtml = DescargadorHtml(),
    private val extractor: ExtractorTresCapas = ExtractorTresCapas(),
    private val almacenamiento: AlmacenamientoArticulos? = null
) {

    private var articuloActivo: ArticuloWeb? = null

    init {
        // Recuperar el último artículo guardado
        articuloActivo = almacenamiento?.obtenerUltimo()
    }

    fun obtenerArticuloActivo(): ArticuloWeb? = articuloActivo

    fun fijarArticuloActivo(articulo: ArticuloWeb) {
        this.articuloActivo = articulo
        almacenamiento?.guardar(articulo)
    }

    suspend fun procesarYGuardar(urlOTexto: String): Result<ArticuloWeb> = withContext(Dispatchers.IO) {
        // Validar y normalizar la URL
        val validacion = ValidadorUrl.extraerYValidar(urlOTexto)
        val urlValida = when (validacion) {
            is ResultadoValidacionUrl.Valida -> validacion.urlNormalizada
            is ResultadoValidacionUrl.Invalida -> return@withContext Result.failure(
                IllegalArgumentException(validacion.motivo)
            )
        }

        // Descargar el HTML respetando robots.txt
        val resultadoDescarga = descargador.descargar(urlValida)
        val (urlFinal, html, tiempoDescarga) = when (resultadoDescarga) {
            is ResultadoDescarga.Exito -> Triple(
                resultadoDescarga.urlFinal,
                resultadoDescarga.html,
                resultadoDescarga.tiempoMs
            )
            is ResultadoDescarga.Error -> return@withContext Result.failure(
                IllegalStateException(resultadoDescarga.mensajeAccesible, resultadoDescarga.excepcion)
            )
        }

        // Extraer el contenido por capas
        return@withContext try {
            val articulo = extractor.extraerArticulo(
                html = html,
                urlOriginal = urlFinal,
                tiempoDescargaMs = tiempoDescarga
            )

            // Guardar en el almacenamiento local
            almacenamiento?.guardar(articulo, html)
            articuloActivo = articulo

            Result.success(articulo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listarArticulosGuardados(): List<ArticuloWeb> {
        return almacenamiento?.listarTodos() ?: emptyList()
    }
}
