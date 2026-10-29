# Estudio de Sitios Web y Diseño del Corpus de Prueba — VoxAccess

Documento técnico de preparación (**Sprint 0 / Sprint 1**) que sustenta la decisión de diseño de **extracción en capas** y la meta de calidad del **90 % de exactitud** establecida en la sección 6 del RAD.

---

## 1. Objetivo y metodología de la investigación

Antes de iniciar la codificación del extractor en el **Sprint 2**, se realizó una auditoría técnica sobre **20 sitios web reales** de distintas categorías (noticias de México, prensa internacional, tecnología, divulgación científica, salud, educación/universidades, enciclopedias con tablas y portales gubernamentales, tanto en español como en inglés).

En cada sitio se verificó:
1. **Cumplimiento de `robots.txt` (RFC 9309):** consulta previa de `https://<dominio>/robots.txt` evaluando las reglas `Allow` y `Disallow` para `User-agent: VoxAccess` y `User-agent: *`.
2. **Respuesta HTTP con User-Agent honesto:** solicitud usando el identificador académico `VoxAccess/1.0 (corpus de pruebas academico; Universidad Politecnica de Penjamo)`, sin falsificar cabeceras de navegador ni evadir protecciones anti-bot, CAPTCHA o muros de pago (RAD sección 6, *Cumplimiento*).
3. **Presencia de metadatos estructurados (Capa 1):** número de bloques `<script type="application/ld+json">` y existencia del campo `articleBody` dentro de la entidad `NewsArticle`, `Article` o `BlogPosting`.
4. **Estructura del DOM para algoritmos de lectura (Capa 2):** número de párrafos `<p>`, volumen de texto visible y presencia de ruido (menús, notas relacionadas, avisos de suscripción).
5. **Presencia de tablas HTML (`<table>`):** detección de tablas de datos para validar el componente `ConversorTablasAccesibles`.

---

## 2. Resumen de hallazgos clave para la arquitectura

- **De los 20 sitios evaluados, 17 resultaron aptos (HTTP 200 y permitidos por `robots.txt`) y 3 fueron descartados** siguiendo las reglas de cumplimiento ético y técnico del proyecto.
- **Ninguna capa por sí sola resuelve todos los sitios (justificación empírica de la arquitectura en capas):**
  - Solo **7 de los 17 sitios aptos (41.2 %)** incluyen el cuerpo completo del artículo en la propiedad `articleBody` de `JSON-LD` (*El País, Expansión, Infobae, Xataka, Genbeta, Hipertextual y Agencia SINC*). En estos sitios, la **Capa 1** entrega el texto limpio de forma directa sin ruido del DOM.
  - **8 de los 17 sitios (47.1 %)** publican `JSON-LD` únicamente con metadatos (título, fecha, autor, idioma), pero dejan el cuerpo exclusivamente en el HTML (*BBC Mundo, DW Español, Gaceta UNAM, Noticias ONU, OMS, NPR y Wikipedia en español e inglés*). En ellos, la **Capa 1** aporta los metadatos confiables y la **Capa 2 (`Readability4J`)** extrae el cuerpo.
  - **2 de los 17 sitios (11.8 %)** no publican ningún bloque `JSON-LD` (*Gob.mx SRE y Animal Político / Grupo Animal*). En ellos, la extracción depende al 100 % de la **Capa 2 (`Readability4J` + `Jsoup`)**.
- **Conversión de tablas:** los artículos de enciclopedia (*Wikipedia en español: Pénjamo* y *Wikipedia en inglés: Screen reader*) contienen entre 2 y 4 elementos `<table>` (incluyendo tablas climáticas y comparativas `wikitable`), lo que justifica el componente `ConversorTablasAccesibles`.
- **Detección de idioma:** el corpus incluye 15 artículos en español (`es`) y 2 en inglés (`en`), permitiendo verificar el cambio automático de voz en `ControladorTTS`.

---

## 3. Catálogo de los 17 sitios aptos del corpus de prueba

| # | ID del caso | Sitio / Portal | Categoría | Idioma | `robots.txt` | Bloques JSON-LD | `articleBody` en JSON-LD | Tablas `<table>` | Párrafos `<p>` / Tamaño | Capa que extrae el cuerpo |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `animalpolitico_taddei_ine` | Animal Político (Grupo Animal) | Noticias México | `es` | Permitido | 0 | No | 0 | 53 (`13.3 KB`) | **Capa 2** (Readability) |
| 2 | `expansion_hecho_en_mexico` | Expansión | Economía México | `es` | Permitido (`Allow: /`) | 3 | **Sí** | 0 | 43 (`7.3 KB`) | **Capa 1** (JSON-LD) |
| 3 | `bbc_mundo_rancheros_nebraska` | BBC News Mundo | Noticias Internacional | `es` | Permitido | 1 | No | 0 | 76 (`11.5 KB`) | **Capa 1** (meta) + **Capa 2** |
| 4 | `elpais_linchamiento_chiapas` | El País México | Noticias Internacional | `es` | Permitido | 4 | **Sí** | 0 | 10 (`6.3 KB`) | **Capa 1** (JSON-LD) |
| 5 | `dw_israel_oposicion` | DW Español | Noticias Internacional | `es` | Permitido | 1 | No | 0 | 20 (`4.8 KB`) | **Capa 1** (meta) + **Capa 2** |
| 6 | `infobae_pymes_ia` | Infobae | Opinión / LatAm | `es` | Permitido | 3 | **Sí** | 0 | 20 (`4.6 KB`) | **Capa 1** (JSON-LD) |
| 7 | `xataka_clinton_anthropic` | Xataka | Tecnología / Seguridad | `es` | Permitido | 1 | **Sí** | 0 | 40 (`24.8 KB`) | **Capa 1** (JSON-LD) |
| 8 | `genbeta_ia_filtran_datos` | Genbeta | Tecnología / IA | `es` | Permitido | 1 | **Sí** | 0 | 42 (`6.0 KB`) | **Capa 1** (JSON-LD) |
| 9 | `hipertextual_apple_macos` | Hipertextual | Tecnología | `es` | Permitido | 1 | **Sí** | 0 | 12 (`3.3 KB`) | **Capa 1** (JSON-LD) |
| 10 | `sinc_anatomia_lenguas` | Agencia SINC | Ciencia | `es` | Permitido (`Allow: /`) | 1 | **Sí** | 0 | 23 (`5.9 KB`) | **Capa 1** (JSON-LD) |
| 11 | `gaceta_unam_villanueva` | Gaceta UNAM | Educación / Universidad | `es` | Permitido | 4 (`@graph`) | No | 0 | 25 (`4.2 KB`) | **Capa 1** (meta) + **Capa 2** |
| 12 | `oms_ceguera` | OMS / WHO (Español) | Salud / Accesibilidad | `es` | Permitido | 3 | No | 0 | 59 (`18.9 KB`) | **Capa 1** (meta) + **Capa 2** |
| 13 | `onu_sarampion_americas` | Noticias ONU | Institucional | `es` | Permitido | 1 | No | 0 | 27 (`6.2 KB`) | **Capa 1** (meta) + **Capa 2** |
| 14 | `gobmx_sre_trafico_armas` | Gob.mx (SRE) | Gobierno de México | `es` | Permitido | 0 | No | 0 | 10 (`1.9 KB`) | **Capa 2** (Readability) |
| 15 | `wikipedia_es_penjamo` | Wikipedia en español (*Pénjamo*) | Enciclopedia / Educación | `es` | Permitido | 1 | No | **4** (`1 wikitable`) | 29 (`9.2 KB`) | **Capa 2** + **Conversor Tablas** |
| 16 | `wikipedia_en_screen_reader` | Wikipedia en inglés (*Screen reader*) | Enciclopedia (Inglés) | `en` | Permitido | 1 | No | **2** | 15 (`7.1 KB`) | **Capa 2** + **Conversor Tablas** |
| 17 | `npr_psychedelics_inflammation` | NPR | Ciencia / Salud (Inglés) | `en` | Permitido | 1 | No | 0 | 29 (`5.9 KB`) | **Capa 1** (meta) + **Capa 2** |

---

## 4. Detalle de URLs y retos específicos de limpieza por sitio

1. **`animalpolitico_taddei_ine`**
   - **URL:** https://animalpolitico.com/politica/taddei-renuncia-ine-presion-politica-auditorias-contratos
   - **Reto de extracción:** redirige al dominio `grupoanimal.mx`; carece de `JSON-LD` y contiene bloques intermedios de *"Te recomendamos"* e inserciones de redes sociales que deben limpiarse en la Capa 2.
2. **`expansion_hecho_en_mexico`**
   - **URL:** https://expansion.mx/economia/2026/10/26/hecho-en-mexico-reglas-de-origen-t-mec
   - **Reto de extracción:** incluye cifras con separadores de miles (`298,655 millones`) y siglas (`T-MEC`, `EU`, `Inegi`) que prueban la no fragmentación indebida en `SegmentadorOraciones`.
3. **`bbc_mundo_rancheros_nebraska`**
   - **URL:** https://www.bbc.com/mundo/articles/cwm235lgg69lo
   - **Reto de extracción:** el HTML incluye 76 párrafos con pies de foto (`Fuente de la imagen, Getty Images`) y enlaces intercalados de *"Lee también"* que deben filtrarse antes de pasar al sintetizador.
4. **`elpais_linchamiento_chiapas`**
   - **URL:** https://elpais.com/mexico/2026-10-27/el-linchamiento-a-los-presuntos-feminicidas-de-una-abuela-y-una-nieta-en-chiapas-enfrenta-a-mexico-a-su-espiral-de-violencia.html
   - **Reto de extracción:** publica 4 bloques `JSON-LD` distintos (`Organization`, `WebSite`, `BreadcrumbList` y `NewsArticle`); el extractor debe iterar todos los bloques hasta localizar el tipo `NewsArticle`.
5. **`dw_israel_oposicion`**
   - **URL:** https://www.dw.com/es/tras-elecciones-en-israel-oposici%C3%B3n-aboga-por-alianza-judeo%C3%A1rabe-para-desbancar-a-netanyahu/a-79787075
   - **Reto de extracción:** contiene llamadas a la acción al final del artículo (*"No dejes que el algoritmo oculte las noticias..."*) y etiquetas de agencias (`rml (efe, afp)`), además de la abreviatura `EE. UU.`.
6. **`infobae_pymes_ia`**
   - **URL:** https://www.infobae.com/opinion/2026/10/24/pymes-e-ia-el-desafio-de-la-adopcion-digital/
   - **Reto de extracción:** columna de opinión con subtítulos (`<h2>`) y múltiples bloques `JSON-LD`.
7. **`xataka_clinton_anthropic`**
   - **URL:** https://www.xataka.com/seguridad/van-a-empeorar-mucho-jason-clinton-subdirector-ciberseguridad-anthropic-advierte-que-esta-venir
   - **Reto de extracción:** artículo extenso (`24.8 KB` de texto) con tarjetas incrustadas de *"En Xataka | ..."* en medio del DOM; el campo `articleBody` de `JSON-LD` permite obtener el texto limpio sin esas tarjetas.
8. **`genbeta_ia_filtran_datos`**
   - **URL:** https://www.genbeta.com/inteligencia-artificial/investigadores-espanoles-demuestran-que-chatgpt-gemini-claude-otras-ia-filtran-datos-chats-a-anunciantes
   - **Reto de extracción:** contiene listas con paréntesis y nombres técnicos en inglés dentro de un artículo en español.
9. **`hipertextual_apple_macos`**
   - **URL:** https://hipertextual.com/apple/apple-blinda-macos-incidente-ia-meta-muse/
   - **Reto de extracción:** comillas tipográficas (`“...”`) y guiones largos (`—`) en citas textuales.
10. **`sinc_anatomia_lenguas`**
    - **URL:** https://www.agenciasinc.es/Noticias/Nuestra-anatomia-podria-explicar-por-que-las-lenguas-suenan-distintas
    - **Reto de extracción:** incluye bloques de licencia Creative Commons 4.0 y metadatos de fuente al pie que no deben leerse como parte del cuerpo.
11. **`gaceta_unam_villanueva`**
    - **URL:** https://www.gaceta.unam.mx/ernesto-villanueva-investigador-de-la-unam-recibe-doctorado-honoris-causa/
    - **Reto de extracción:** usa el plugin Yoast SEO con un único objeto `@graph` que anida `WebPage`, `Article`, `Person` y `Organization` sin `articleBody`.
12. **`oms_ceguera`**
    - **URL:** https://www.who.int/es/news-room/fact-sheets/detail/blindness-and-visual-impairment
    - **Reto de extracción:** hoja informativa estructurada con listas de viñetas (`<ul><li>`) sobre datos y cifras de discapacidad visual; el extractor debe conservar los elementos de lista como oraciones completas.
13. **`onu_sarampion_americas`**
    - **URL:** https://news.un.org/es/story/2026/10/1542035
    - **Reto de extracción:** página con menús extensos de navegación por regiones y temas de la ONU que Readability debe aislar.
14. **`gobmx_sre_trafico_armas`**
    - **URL:** https://www.gob.mx/sre/prensa/mexico-logra-adopcion-unanime-de-resolucion-para-fortalecer-el-combate-contra-el-trafico-de-armas-de-fuego
    - **Reto de extracción:** portal gubernamental mexicano sin `JSON-LD` y con numerales ordinales (`81.° periodo de sesiones`).
15. **`wikipedia_es_penjamo`**
    - **URL:** https://es.wikipedia.org/wiki/P%C3%A9njamo
    - **Reto de extracción:** contiene referencias entre corchetes (`[1]`, `[2]`), botón `[editar]` en cada encabezado y 4 tablas (ficha geográfica y tabla climática `wikitable`). El preprocesador debe eliminar `[editar]` y referencias numéricas, y convertir las tablas por fila.
16. **`wikipedia_en_screen_reader`**
    - **URL:** https://en.wikipedia.org/wiki/Screen_reader
    - **Reto de extracción:** artículo en inglés (`lang="en"`) con tablas comparativas de lectores de pantalla; valida tanto el cambio automático a voz en inglés como la lectura accesible de tablas.
17. **`npr_psychedelics_inflammation`**
    - **URL:** https://www.npr.org/2026/10/21/nx-s1-6009441/psychedelics-brain-inflammation-immune-system
    - **Reto de extracción:** artículo en inglés con botones de reproductor de audio (*"Listen · 4:38"*) y avisos institucionales de NPR que deben quedar fuera del cuerpo extraído.

---

## 5. Sitios descartados y justificación de cumplimiento

Conforme a la sección 6 del RAD (*Cumplimiento: "La aplicación no evade pagos, inicios de sesión ni CAPTCHA; respeta las condiciones de uso de cada sitio y el protocolo robots.txt"*), se documentan los 3 sitios descartados durante el estudio:

| Sitio evaluado | URL consultada | Respuesta HTTP | Causa técnica | Comportamiento esperado en VoxAccess V1 |
|---|---|---|---|---|
| **La Jornada** | `https://www.jornada.com.mx/noticia/2026/10/28/economia/...` | `HTTP 403` | Protección Cloudflare Turnstile (`<title>Just a moment...</title>`) que exige ejecución de JavaScript y resolución de reto anti-bot. | No evadir el reto; anunciar: *"El sitio bloqueó la descarga automática (código 403). Intenta con otro portal."* |
| **Medium** | `https://consuelocorrea.medium.com/los-overlay-no-cumplen...` | `HTTP 403` | Reto Cloudflare para clientes HTTP directos fuera de un navegador interactivo. | No falsificar cookies ni cabeceras; informar de manera accesible que el sitio requiere verificación en el navegador. |
| **Concepto.de** | `https://concepto.de/discapacidad/` | `HTTP 404` | Recurso no encontrado en la ruta solicitada (`404 - Página no encontrada`). | Detectar el código HTTP $404$ antes de extraer y anunciar: *"La página solicitada no existe (error 404)."* |

---

## 6. Métrica de evaluación para la meta del 90 %

Durante el **Sprint 2**, la prueba automatizada `ReporteExactitudTest` evaluará cada uno de los 17 sitios aptos calculando:

$$\text{Exactitud del sitio} = 0.7 \times \left(\frac{\text{Fragmentos clave del cuerpo recuperados}}{\text{Total de fragmentos clave esperados}}\right) + 0.3 \times \left(\frac{\text{Fragmentos de ruido excluidos}}{\text{Total de fragmentos de ruido medidos}}\right)$$

El criterio de aceptación del subsistema de extracción exige que el **promedio de exactitud en los 17 sitios sea $\ge 90\%$**.
