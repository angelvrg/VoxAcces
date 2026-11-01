package com.voxaccess.core.validator

import java.net.URI
import java.util.regex.Pattern

sealed class ResultadoValidacionUrl {
    data class Valida(val urlNormalizada: String) : ResultadoValidacionUrl()
    data class Invalida(val motivo: String) : ResultadoValidacionUrl()
}

object ValidadorUrl {

    private val PATRON_URL_TEXTO = Pattern.compile(
        "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\))+(?:\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))"
    )

    private val IPS_PRIVADAS = listOf(
        "127.",
        "10.",
        "192.168.",
        "172.16.", "172.17.", "172.18.", "172.19.",
        "172.20.", "172.21.", "172.22.", "172.23.",
        "172.24.", "172.25.", "172.26.", "172.27.",
        "172.28.", "172.29.", "172.30.", "172.31.",
        "0.0.0.0", "localhost"
    )

    /**
     * Busca y valida la primera URL de un texto compartido o pegado.
     */
    fun extraerYValidar(texto: String): ResultadoValidacionUrl {
        val limpia = texto.trim()
        if (limpia.isEmpty()) {
            return ResultadoValidacionUrl.Invalida("El texto ingresado está vacío.")
        }

        // Si el texto ya es una URL
        val urlCandidata = if (limpia.startsWith("http://", ignoreCase = true) ||
            limpia.startsWith("https://", ignoreCase = true)
        ) {
            limpia.split("\\s+".toRegex()).firstOrNull() ?: limpia
        } else {
            // Buscar una URL dentro del texto
            val matcher = PATRON_URL_TEXTO.matcher(limpia)
            if (matcher.find()) {
                matcher.group(1)
            } else {
                return ResultadoValidacionUrl.Invalida(
                    "El enlace ingresado no es válido. Ingresa una dirección web pública con HTTPS."
                )
            }
        }

        return validarUrlDirecta(urlCandidata)
    }

    /**
     * Valida una URL con HTTPS, dominio público y esquema permitido.
     */
    fun validarUrlDirecta(urlStr: String): ResultadoValidacionUrl {
        var procesada = urlStr.trim()

        if (procesada.startsWith("javascript:", ignoreCase = true) ||
            procesada.startsWith("file:", ignoreCase = true) ||
            procesada.startsWith("content:", ignoreCase = true) ||
            procesada.startsWith("data:", ignoreCase = true)
        ) {
            return ResultadoValidacionUrl.Invalida(
                "Esquema no permitido. Solo se permiten páginas web públicas HTTPS."
            )
        }

        // Agregar https:// si falta el esquema
        if (!procesada.startsWith("http://", ignoreCase = true) &&
            !procesada.startsWith("https://", ignoreCase = true)
        ) {
            procesada = "https://$procesada"
        }

        // Forzar HTTPS
        if (procesada.startsWith("http://", ignoreCase = true)) {
            procesada = "https://" + procesada.substring(7)
        }

        return try {
            val uri = URI(procesada)
            val host = uri.host ?: return ResultadoValidacionUrl.Invalida(
                "La dirección web no contiene un nombre de dominio válido."
            )

            // Rechazar IPs privadas y localhost
            val hostLower = host.lowercase()
            if (IPS_PRIVADAS.any { hostLower.startsWith(it) || hostLower == it }) {
                return ResultadoValidacionUrl.Invalida(
                    "No se permiten direcciones locales ni privadas de red."
                )
            }

            if (!hostLower.contains(".")) {
                return ResultadoValidacionUrl.Invalida(
                    "El dominio no parece válido. Debe incluir un sufijo público (ej. .com, .org, .mx)."
                )
            }

            ResultadoValidacionUrl.Valida(uri.toASCIIString())
        } catch (e: Exception) {
            ResultadoValidacionUrl.Invalida(
                "El formato de la dirección web es inválido: ${e.message}"
            )
        }
    }
}
