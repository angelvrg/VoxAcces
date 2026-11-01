package com.voxaccess.core.network

import com.voxaccess.core.compliance.RobotsTxt
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class ResultadoDescarga {
    data class Exito(
        val urlFinal: String,
        val html: String,
        val tiempoMs: Long,
        val contentType: String?
    ) : ResultadoDescarga()

    data class Error(
        val codigoHttp: Int? = null,
        val mensajeAccesible: String,
        val excepcion: Throwable? = null
    ) : ResultadoDescarga()
}

class DescargadorHtml(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
    private val verificarRobots: Boolean = true
) {

    companion object {
        const val USER_AGENT_ACADEMICO =
            "VoxAccess/1.0 (corpus de pruebas academico; Universidad Politecnica de Penjamo)"
    }

    private val cacheRobots = ConcurrentHashMap<String, RobotsTxt>()

    fun descargar(url: String): ResultadoDescarga {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return ResultadoDescarga.Error(
                mensajeAccesible = "La dirección web es inválida.",
                excepcion = e
            )
        }

        // Verificar robots.txt antes de descargar
        if (verificarRobots) {
            val host = uri.host ?: ""
            val robots = cacheRobots.getOrPut(host) {
                descargarRobotsTxt(url)
            }

            val ruta = (uri.rawPath ?: "/") + if (uri.rawQuery != null) "?${uri.rawQuery}" else ""
            if (!robots.esRutaPermitida(ruta, "VoxAccess")) {
                return ResultadoDescarga.Error(
                    codigoHttp = 403,
                    mensajeAccesible = "El sitio web no permite la lectura automática de esta sección según su archivo robots.txt."
                )
            }
        }

        // Descargar el artículo
        val inicio = System.currentTimeMillis()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT_ACADEMICO)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "es-MX,es;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                procesarRespuesta(response, inicio)
            }
        } catch (e: IOException) {
            ResultadoDescarga.Error(
                mensajeAccesible = "No hay conexión a internet. Verifica tu red Wi-Fi o datos móviles e inténtalo de nuevo.",
                excepcion = e
            )
        } catch (e: Exception) {
            ResultadoDescarga.Error(
                mensajeAccesible = "Ocurrió un error inesperado al descargar el artículo: ${e.message}",
                excepcion = e
            )
        }
    }

    private fun procesarRespuesta(response: Response, inicioMs: Long): ResultadoDescarga {
        val codigo = response.code
        val tiempoTotal = System.currentTimeMillis() - inicioMs

        if (!response.isSuccessful) {
            val mensaje = when (codigo) {
                401, 403 -> "El sitio web restringe la descarga automática de este artículo (código $codigo). Puede requerir suscripción o verificación."
                404 -> "La página solicitada no existe o fue retirada (error 404)."
                500, 502, 503, 504 -> "El servidor del sitio web presenta fallas temporales (error $codigo). Intenta más tarde."
                else -> "No fue posible obtener el artículo del servidor (código HTTP $codigo)."
            }
            return ResultadoDescarga.Error(codigoHttp = codigo, mensajeAccesible = mensaje)
        }

        val body = response.body
            ?: return ResultadoDescarga.Error(
                codigoHttp = codigo,
                mensajeAccesible = "La página web descargada está vacía."
            )

        val html = body.string()
        val urlFinal = response.request.url.toString()

        // Detectar verificación anti-bot de Cloudflare
        if (html.contains("<title>Just a moment...</title>", ignoreCase = true) ||
            html.contains("Attention Required! | Cloudflare", ignoreCase = true)
        ) {
            return ResultadoDescarga.Error(
                codigoHttp = 403,
                mensajeAccesible = "El sitio web solicita verificación anti-bot en el navegador interactivo."
            )
        }

        return ResultadoDescarga.Exito(
            urlFinal = urlFinal,
            html = html,
            tiempoMs = tiempoTotal,
            contentType = response.header("Content-Type")
        )
    }

    private fun descargarRobotsTxt(urlArticulo: String): RobotsTxt {
        return try {
            val urlRobots = RobotsTxt.obtenerUrlRobots(urlArticulo)
            val req = Request.Builder()
                .url(urlRobots)
                .header("User-Agent", USER_AGENT_ACADEMICO)
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful && res.body != null) {
                    val texto = res.body!!.string()
                    RobotsTxt.parsear(texto)
                } else {
                    RobotsTxt.PERMITIR_TODO
                }
            }
        } catch (e: Exception) {
            RobotsTxt.PERMITIR_TODO
        }
    }
}
