package com.voxaccess.core.extractor

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Convierte tablas HTML en listas secuenciales comprensibles para lectores de pantalla.
 */
class ConversorTablasAccesibles {

    fun convertirTablasEnDocumento(doc: Document): Int {
        val tablas = doc.select("table")
        var convertidas = 0

        for (tabla in tablas) {
            if (esTablaDatos(tabla)) {
                val textoAccesible = transformarTablaATexto(tabla)
                if (textoAccesible.isNotEmpty()) {
                    val pReemplazo = doc.createElement("p")
                    pReemplazo.attr("class", "vox-tabla-accesible")
                    pReemplazo.text(textoAccesible)
                    tabla.replaceWith(pReemplazo)
                    convertidas++
                }
            } else {
                // Tabla de maquetación: conservar su texto o eliminarla si está vacía
                val texto = tabla.text().trim()
                if (texto.isEmpty()) {
                    tabla.remove()
                }
            }
        }

        return convertidas
    }

    private fun esTablaDatos(tabla: Element): Boolean {
        if (tabla.attr("role").equals("presentation", ignoreCase = true) ||
            tabla.attr("role").equals("none", ignoreCase = true)
        ) {
            return false
        }

        // Con encabezados o título, se considera tabla de datos
        if (tabla.select("th").isNotEmpty() || tabla.select("caption").isNotEmpty()) {
            return true
        }

        val filas = tabla.select("tr")
        if (filas.size >= 2) {
            val celdasPrimeraFila = filas[0].select("td, th")
            if (celdasPrimeraFila.size >= 2) {
                return true
            }
        }

        return false
    }

    private fun transformarTablaATexto(tabla: Element): String {
        val sb = StringBuilder()

        val caption = tabla.selectFirst("caption")?.text()?.trim()
        val tituloTabla = if (!caption.isNullOrEmpty()) "Tabla: $caption. " else "Tabla de datos. "
        sb.append(tituloTabla)

        val filas = tabla.select("tr")
        if (filas.isEmpty()) return ""

        // Nombres de las columnas
        val encabezados = mutableListOf<String>()
        val thElements = tabla.select("th")

        val primeraFila = filas.firstOrNull()
        if (thElements.isNotEmpty()) {
            val filaEncabezados = filas.firstOrNull { it.select("th").isNotEmpty() }
            filaEncabezados?.select("th, td")?.forEach { th ->
                encabezados.add(th.text().trim())
            }
        } else if (primeraFila != null) {
            primeraFila.select("td").forEach { td ->
                encabezados.add(td.text().trim())
            }
        }

        var numFila = 1
        for (i in filas.indices) {
            val fila = filas[i]

            // Omitir la fila que solo contiene encabezados
            if (i == 0 && fila.select("th").size == fila.select("th, td").size && filas.size > 1) {
                continue
            }

            val celdas = fila.select("td, th")
            if (celdas.isEmpty()) continue

            val fragmentosCeldas = mutableListOf<String>()

            for (j in celdas.indices) {
                val celdaTexto = celdas[j].text().trim()
                if (celdaTexto.isEmpty()) continue

                val nombreColumna = encabezados.getOrNull(j)
                if (!nombreColumna.isNullOrEmpty() && !nombreColumna.equals(celdaTexto, ignoreCase = true)) {
                    fragmentosCeldas.add("$nombreColumna: $celdaTexto")
                } else {
                    fragmentosCeldas.add(celdaTexto)
                }
            }

            if (fragmentosCeldas.isNotEmpty()) {
                sb.append("Fila $numFila: ")
                sb.append(fragmentosCeldas.joinToString("; "))
                sb.append(". ")
                numFila++
            }
        }

        return sb.toString().trim()
    }
}
