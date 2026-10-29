# Product Backlog y Planificación de Sprints — VoxAccess

Documento de preparación correspondiente al **Sprint 0** y **Sprint 1** de la **MiniHackatón 2026 · Mobile Software Challenge**.  
Trazable con el documento técnico **RAD v1.0** y el **SRS** del proyecto.

---

## 1. Definición de Terminado (Definition of Done — DoD)

De acuerdo con la sección 3.5 del RAD, una Historia de Usuario se considera **Terminada** únicamente cuando cumple todos los puntos siguientes:

1. **Funcionalidad de extremo a extremo:** opera sin cierres inesperados en un teléfono Android real (`minSdk 26` o superior).
2. **Accesibilidad verificada:** es 100 % operable con **TalkBack** activado, cumple **WCAG 2.2 Nivel AA** (contraste $\ge 4.5:1$ en texto, $\ge 3:1$ en controles, áreas táctiles $\ge 48 \times 48\text{ dp}$) y emite anuncios de estado oportunos.
3. **Pruebas superadas:** aprueba sus pruebas unitarias en JUnit (para componentes de extracción, validación y segmentación) y los casos de prueba manuales definidos en [`docs/CASOS_DE_PRUEBA.md`](CASOS_DE_PRUEBA.md).
4. **Privacidad y cumplimiento:** procesa el contenido localmente en el dispositivo, no solicita credenciales, no almacena datos personales reales, descarga solo por HTTPS y respeta `robots.txt` sin evadir muros de pago ni CAPTCHA.
5. **Trazabilidad y defensa técnica:** las bibliotecas y herramientas empleadas están declaradas en el expediente y ambos integrantes pueden explicar y defender las decisiones de diseño.

---

## 2. Trazabilidad entre Requisitos, Casos de Uso e Historias de Usuario

| ID Requisito | Descripción resumida | Caso de Uso (UML) | Historias de Usuario asociadas | Compromiso |
|---|---|---|---|---|
| **RF-01** | Captura del artículo por URL (pegar o menú Compartir) y validación HTTPS/`robots.txt` | **UC1** · Compartir o pegar la URL «V1» | HU-01, HU-02 | **V1** |
| **RF-02** | Extracción del texto principal en capas (JSON-LD + Readability + tablas) | **UC2** · Escuchar el artículo en voz alta «V1» | HU-03, HU-04 | **V1** |
| **RF-03** | Lectura en voz alta con el motor TTS del sistema en menos de 3 s | **UC2** · Escuchar el artículo en voz alta «V1» | HU-05, HU-08 | **V1** |
| **RF-04** | Controles de reproducción: pausar, reanudar y velocidad ($0.75\times$ a $2.5\times$) con Media3 | **UC3** · Controlar la reproducción «V1» | HU-06 | **V1** |
| **RF-05** | Segmentación y navegación por oraciones (anterior, siguiente, frase exacta) | **UC3** · Controlar la reproducción «V1» | HU-07 | **V1** |
| **RF-06** | Interfaz accesible con anuncios de estado para el lector de pantalla | Transversal (UC1, UC2, UC3) | HU-08 | **V1** |
| **RF-07** | Resumen breve local con IA en el dispositivo (`ONNX` / `MediaPipe`) | **UC4** · Solicitar un resumen breve (fase 2) | HU-09 | *Fase 2* |
| **RF-08** | Guardado y lectura de artículos sin conexión (`RepositorioOffline`) | **UC5 / UC6** · Guardar y leer sin conexión (fase 2) | HU-10 | *Fase 2* |

---

## 3. Product Backlog — Versión Evaluable «V1»

### HU-01 · Capturar enlace pegándolo o desde el menú Compartir
- **Como** persona usuaria de TalkBack,
- **Quiero** pegar el enlace de un artículo en la pantalla de inicio o compartirlo directamente desde el navegador hacia VoxAccess,
- **Para** abrir el artículo en un solo paso sin buscar el texto dentro de la página web.
- **Prioridad:** Alta (Must Have) · **Estimación:** 3 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `GestorAccesibilidadUI`, `ArticuloWeb`.
- **Criterios de aceptación:**
  1. La pantalla de inicio incluye un campo de texto accesible con etiqueta clara, un botón *"Pegar del portapapeles"* y un botón *"Leer artículo"*.
  2. La aplicación registra un `intent-filter` para `android.intent.action.SEND` (`text/plain`); al compartir desde Chrome u otro navegador, extrae la primera URL válida del texto compartido e inicia la carga de inmediato.
  3. Si el enlace usa `http://`, se normaliza a `https://`. Si el formato es inválido o apunta a un esquema no permitido (`file://`, `javascript:`, IP privada), se anuncia un error claro por voz y en pantalla.

### HU-02 · Verificar `robots.txt` y descargar el documento por HTTPS
- **Como** equipo responsable del cumplimiento técnico,
- **Quiero** que la aplicación consulte y respete el archivo `robots.txt` del dominio antes de descargar el artículo por HTTPS,
- **Para** cumplir con el estándar RFC 9309 y las políticas de cada portal sin evadir bloqueos ni muros de pago.
- **Prioridad:** Alta (Must Have) · **Estimación:** 3 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `ExtractorTresCapas`.
- **Criterios de aceptación:**
  1. Antes de descargar la página, se evalúan las reglas `Allow` y `Disallow` para `User-agent: VoxAccess` y `User-agent: *`.
  2. Si la ruta está prohibida por `robots.txt`, no se descarga el artículo y se informa a la persona usuaria con un mensaje accesible.
  3. Si el servidor responde `401`, `403`, `404` o presenta un bloqueo anti-bot/muro de pago, la app no intenta evadirlo y notifica la causa de forma comprensible.

### HU-03 · Extraer el texto principal mediante las Capas 1 y 2
- **Como** persona con discapacidad visual,
- **Quiero** que VoxAccess extraiga únicamente el título, autor y cuerpo principal del artículo descartando menús, anuncios, avisos de cookies y notas relacionadas,
- **Para** escuchar el contenido sin distracciones ni ruido de navegación.
- **Prioridad:** Alta (Must Have) · **Estimación:** 8 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `ExtractorTresCapas`, `ArticuloWeb`.
- **Criterios de aceptación:**
  1. **Capa 1 (`JSON-LD`):** extrae `headline`, `author`, `datePublished`, `inLanguage` y `articleBody` de bloques `<script type="application/ld+json">` (incluyendo estructuras con `@graph`). Si `articleBody` está completo ($\ge 300$ caracteres), se utiliza como texto base.
  2. **Capa 2 (`Readability4J` + `Jsoup`):** cuando el sitio no incluye `articleBody` en JSON-LD, limpia los contenedores de navegación, publicidad y pie de página y extrae el cuerpo principal con `Readability4J`.
  3. En el corpus de prueba de 17 sitios reales, alcanza una exactitud promedio $\ge 90\%$ (medida por cobertura de fragmentos del cuerpo y ausencia de fragmentos de ruido).

### HU-04 · Convertir tablas HTML en listas secuenciales accesibles
- **Como** estudiante o lector con discapacidad visual,
- **Quiero** que las tablas de datos dentro de un artículo se transformen en texto secuencial fila por fila asociando cada celda con su encabezado,
- **Para** comprender la información tabular al escucharla de corrido sin perder la referencia de las columnas.
- **Prioridad:** Media-Alta (Should Have en V1 según arquitectura aprobada) · **Estimación:** 3 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `ConversorTablasAccesibles`.
- **Criterios de aceptación:**
  1. Detecta las tablas de datos (`<table>` con encabezados `<th>` o estructura tabular dentro del artículo) y descarta tablas meramente decorativas o de maquetación.
  2. Convierte cada fila al formato: *"Tabla [caption]. Fila 1: [Encabezado 1], [Valor 1]; [Encabezado 2], [Valor 2]."*
  3. Probado con artículos de Wikipedia del corpus que contienen tablas (`wikipedia_es_penjamo` y `wikipedia_en_screen_reader`).

### HU-05 · Segmentar en oraciones, generar SSML e iniciar la voz en menos de 3 s
- **Como** persona usuaria,
- **Quiero** que el artículo comience a leerse en voz alta en menos de 3 segundos con la voz del sistema en el idioma correcto,
- **Para** no esperar largos tiempos de procesamiento antes de escuchar la primera frase.
- **Prioridad:** Alta (Must Have) · **Estimación:** 5 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `ProcesadorSSML`, `Oracion`, `ControladorTTS`.
- **Criterios de aceptación:**
  1. `ProcesadorSSML` divide el texto limpio en objetos `Oracion` numerados, evitando cortes falsos en abreviaturas comunes (`Dr.`, `Sra.`, `EE. UU.`, `etc.`, `pág.`, iniciales y números decimales).
  2. Cada `Oracion` almacena su `textoPlano`, su `marcadoSSML` (`<speak><s>...</s><break time="..."/></speak>`) y su `duracionEstimadaMs`.
  3. `ControladorTTS` detecta el idioma del artículo (`es`, `en`, etc.), selecciona una voz compatible instalada en el dispositivo y pronuncia la primera oración en $< 3\text{ s}$ tras finalizar la descarga.

### HU-06 · Controlar la reproducción (pausa, velocidad y controles externos Media3)
- **Como** persona usuaria,
- **Quiero** pausar, reanudar y cambiar la velocidad de lectura entre $0.75\times$ y $2.5\times$, tanto desde la pantalla como desde mis auriculares o la pantalla de bloqueo,
- **Para** adaptar el ritmo de lectura a mi preferencia sin depender de desbloquear el teléfono.
- **Prioridad:** Alta (Must Have) · **Estimación:** 5 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `ControladorTTS`, `GestorAccesibilidadUI`.
- **Criterios de aceptación:**
  1. Botones grandes en pantalla ($\ge 64\text{ dp}$) permiten alternar Pausa/Reanudar y modificar la velocidad en incrementos de $0.25\times$ dentro del rango $[0.75\times, 2.5\times]$.
  2. Un servicio en primer plano con `MediaSessionService` (`Media3`) expone los controles a la notificación del sistema, pantalla de bloqueo y botones de auriculares.
  3. La lectura se pausa automáticamente si entra una llamada telefónica (foco de audio) o si se desconectan los auriculares.

### HU-07 · Navegar por oraciones y volver a una frase exacta
- **Como** persona usuaria,
- **Quiero** saltar a la oración anterior o siguiente, o tocar cualquier oración de la lista para escuchar desde ese punto,
- **Para** repasar una idea o cita sin tener que reiniciar todo el artículo.
- **Prioridad:** Alta (Must Have) · **Estimación:** 5 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `ControladorTTS`, `Oracion`, `GestorAccesibilidadUI`.
- **Criterios de aceptación:**
  1. Los botones *"Oración anterior"* y *"Oración siguiente"* (y las acciones equivalentes en auriculares/TalkBack) desplazan inmediatamente el índice de lectura en $\pm 1$.
  2. La pantalla del lector muestra la lista de oraciones resaltando la oración activa y haciendo desplazamiento automático suave.
  3. Al activar cualquier oración de la lista con doble toque en TalkBack, la reproducción salta a esa oración exacta.

### HU-08 · Anuncios de estado accesibles y selector de voz en Ajustes
- **Como** persona usuaria de TalkBack,
- **Quiero** recibir anuncios claros del estado del sistema (*"Extrayendo artículo principal…"*, *"Artículo listo"*, mensajes de error) y poder elegir la voz o velocidad predeterminada en Ajustes,
- **Para** saber siempre qué ocurre en la aplicación sin que el lector de pantalla y la voz del artículo se encimen.
- **Prioridad:** Media-Alta (Should Have) · **Estimación:** 3 puntos · **Sprint:** 2
- **Clases UML relacionadas:** `GestorAccesibilidadUI`, `ControladorTTS`.
- **Criterios de aceptación:**
  1. Los cambios de estado se anuncian mediante regiones vivas (`liveRegion = LiveRegionMode.Polite`) sin robar ni desordenar el foco de TalkBack durante la lectura.
  2. Se registran `CustomAccessibilityAction` en la pantalla de lectura para ejecutar *Oración anterior*, *Oración siguiente*, *Aumentar velocidad* y *Disminuir velocidad* desde el menú de acciones de TalkBack.
  3. La pantalla de Ajustes permite elegir entre las voces del motor TTS instaladas en el sistema y definir la velocidad inicial.

---

## 4. Product Backlog — Funcionalidades Futuras «Fase 2» (Excluidas de V1)

| ID | Historia de Usuario | Subsistema / Clases UML | Motivo de programación en Fase 2 |
|---|---|---|---|
| **HU-09** | **Clasificación de bloques residuales con IA local (Capa 3):** filtrar ruido remanente en sitios atípicos mediante un clasificador ligero ejecutado en `ONNX Runtime`. | `ExtractorTresCapas.clasificarBloquesONNX()` | Las Capas 1 y 2 más el conversor de tablas cubren la meta del 90 % en V1 sin incrementar el peso del APK. |
| **HU-10** | **Resumen breve del artículo en el dispositivo:** generar y escuchar una síntesis de 3 a 5 puntos clave usando `ONNX Runtime` / `MediaPipe` sin enviar datos a la nube. | `ResumenLigeroIA` (UC4) | Requiere integrar y cuantizar un modelo generativo local; documentado en RAD 2.2 como fase posterior. |
| **HU-11** | **Guardado y lectura sin conexión:** almacenar el texto limpio, el marcado SSML y la caché de audio en archivos locales para escuchar sin internet. | `RepositorioOffline` (UC5, UC6) | Excluido en RAD 2.2 para priorizar la estabilidad del flujo en línea durante la minihackatón. |
| **HU-12** | **Voces neuronales locales (`Piper` / `Kokoro`):** ofrecer síntesis neuronal offline de alta naturalidad integrada en la app. | `ControladorTTS` | En V1 se utiliza el motor `TextToSpeech` del sistema (gratuito y ya instalado en Android). |

---

## 5. Planificación detallada por Sprint

### Sprint 0 — Miércoles 28 de octubre de 2026
**Objetivo:** inscripción formal, definición del alcance comprometido, creación del repositorio y backlog inicial.
- [x] Definir roles Scrum del equipo (Product Owner y Scrum Master).
- [x] Delimitar el alcance V1 vs. Fase 2 y confirmar el stack tecnológico (Kotlin + Jetpack Compose, Jsoup + Readability4J, Media3, Android TTS, `minSdk 26`).
- [x] Inicializar el repositorio Git (`main`) y estructurar la documentación base (`README.md`, `docs/BACKLOG.md`).

### Sprint 1 — Jueves 29 de octubre de 2026
**Objetivo:** documentación técnica completa (SRS, RAD, diagramas UML, estudio de corpus y casos de prueba).
- [x] Elaborar y revisar el documento técnico RAD v1.0 con los 5 diagramas UML (casos de uso, clases, secuencia, actividades y componentes) marcando explícitamente «V1» y «fase 2».
- [x] Investigar 20 sitios web variados (17 aptos y 3 descartados por bloqueo/CAPTCHA) verificando `robots.txt`, JSON-LD y tablas HTML (`docs/ESTUDIO_SITIOS_CORPUS.md`).
- [x] Especificar los casos de prueba de los 3 flujos críticos y la lista de verificación de accesibilidad (`docs/CASOS_DE_PRUEBA.md`).

### Sprint 2 — Viernes 30 de octubre a lunes 2 de noviembre de 2026
**Objetivo:** diseño UX/UI en Figma, desarrollo del prototipo móvil ejecutable y pruebas de los flujos críticos.
- [ ] **Día 1 (Vie. 30/oct):** crear el esqueleto multimódulo (`:core` y `:app`), implementar `ArticuloWeb`, `Oracion`, `ValidadorUrl`, `RobotsTxt`, `ExtractorJsonLd`, `ExtractorReadability`, `ConversorTablasAccesibles`, `SegmentadorOraciones` y `ProcesadorSSML`, y ejecutar las pruebas JUnit contra el corpus de 17 sitios.
- [ ] **Día 2 (Sáb. 31/oct):** implementar `DescargadorHtml` (OkHttp + HTTPS + `robots.txt`), `RepositorioArticulos`, `PreferenciasRepo` y `ControladorTTS` (oración por oración, detección de idioma, velocidad $0.75\times$ a $2.5\times$).
- [ ] **Día 3 (Dom. 01/nov):** integrar `TtsPlayer` y `ServicioLectura` con `Media3`, implementar las pantallas accesibles en Compose (`InicioScreen`, `LectorScreen`, `AjustesScreen`) y el receptor del menú *Compartir* (`ACTION_SEND`).
- [ ] **Día 4 (Lun. 02/nov):** aplicar los estilos y componentes del diseño de Figma, ejecutar los casos de prueba en teléfono real con TalkBack activado y cerrar el incremento funcional V1.

### Sprint 3 — Martes 3 a jueves 5 de noviembre de 2026
**Objetivo:** versión funcional evaluable, video de demostración, pulido final y defensa técnica.
- [ ] Ajustar detalles de accesibilidad y tiempos de respuesta detectados en las pruebas con dispositivo real.
- [ ] Generar el reporte final de exactitud del extractor sobre el corpus ($\ge 90\%$) para el expediente técnico.
- [ ] Grabar el video de demostración mostrando los 3 flujos críticos con TalkBack activo y datos ficticios/públicos.
- [ ] Preparar la defensa técnica del diseño, arquitectura y decisiones del RAD.
