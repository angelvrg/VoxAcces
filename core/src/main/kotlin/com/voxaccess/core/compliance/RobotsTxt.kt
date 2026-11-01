package com.voxaccess.core.compliance

import java.net.URI

/**
 * Evalúa el cumplimiento del protocolo de exclusión de robots (RFC 9309).
 */
class RobotsTxt(private val reglasPorAgente: Map<String, List<ReglaRuta>>) {

    data class ReglaRuta(val esPermitido: Boolean, val prefijoRuta: String)

    /**
     * Indica si una ruta está permitida para VoxAccess o para agentes generales (*).
     */
    fun esRutaPermitida(ruta: String, agente: String = "VoxAccess"): Boolean {
        val rutaLimpia = if (ruta.startsWith("/")) ruta else "/$ruta"

        // Reglas específicas para VoxAccess
        reglasPorAgente[agente.lowercase()]?.let { reglas ->
            val resultado = evaluarReglas(reglas, rutaLimpia)
            if (resultado != null) return resultado
        }

        // Reglas comodín para cualquier agente
        reglasPorAgente["*"]?.let { reglas ->
            val resultado = evaluarReglas(reglas, rutaLimpia)
            if (resultado != null) return resultado
        }

        // Sin reglas aplicables, se permite por defecto
        return true
    }

    private fun evaluarReglas(reglas: List<ReglaRuta>, ruta: String): Boolean? {
        // Gana la regla más específica (la más larga)
        val reglaCoincidente = reglas
            .filter { regla ->
                if (regla.prefijoRuta.isEmpty()) {
                    false
                } else if (regla.prefijoRuta.endsWith("$")) {
                    val sinDolar = regla.prefijoRuta.dropLast(1)
                    ruta == sinDolar
                } else {
                    ruta.startsWith(regla.prefijoRuta)
                }
            }
            .maxByOrNull { it.prefijoRuta.length }

        return reglaCoincidente?.esPermitido
    }

    companion object {
        val PERMITIR_TODO = RobotsTxt(emptyMap())

        /**
         * Lee el contenido de un archivo robots.txt.
         */
        fun parsear(contenido: String): RobotsTxt {
            val mapa = mutableMapOf<String, MutableList<ReglaRuta>>()
            val lineas = contenido.lines()

            var agentesActuales = mutableListOf<String>()

            for (linea in lineas) {
                val lineaSinComentarios = linea.substringBefore('#').trim()
                if (lineaSinComentarios.isEmpty()) continue

                val partes = lineaSinComentarios.split(":", limit = 2)
                if (partes.size < 2) continue

                val directiva = partes[0].trim().lowercase()
                val valor = partes[1].trim()

                when (directiva) {
                    "user-agent" -> {
                        val agente = valor.lowercase()
                        agentesActuales.add(agente)
                    }
                    "disallow" -> {
                        if (valor.isNotEmpty()) {
                            for (agente in agentesActuales) {
                                mapa.getOrPut(agente) { mutableListOf() }
                                    .add(ReglaRuta(esPermitido = false, prefijoRuta = valor))
                            }
                        }
                    }
                    "allow" -> {
                        if (valor.isNotEmpty()) {
                            for (agente in agentesActuales) {
                                mapa.getOrPut(agente) { mutableListOf() }
                                    .add(ReglaRuta(esPermitido = true, prefijoRuta = valor))
                            }
                        }
                    }
                }
            }

            return RobotsTxt(mapa)
        }

        fun obtenerUrlRobots(urlArticulo: String): String {
            val uri = URI(urlArticulo)
            val puertoStr = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            return "${uri.scheme}://${uri.host}$puertoStr/robots.txt"
        }
    }
}
