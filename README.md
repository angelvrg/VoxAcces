# VoxAccess — Lector web para personas con discapacidad visual

**Universidad Politécnica de Pénjamo**  
Ingeniería en Tecnologías de la Información e Innovación Digital  
*Análisis y Diseño de Software · MiniHackatón 2026 — Mobile Software Challenge*  
**Vertical:** Bienestar social — Accesibilidad

---

## 1. Descripción del proyecto

**VoxAccess** es una aplicación móvil para Android que complementa a lectores de pantalla como **TalkBack** al abrir artículos web de noticias, ciencia y materiales educativos sin recorrer menús, anuncios ni elementos distractores.

A partir de un enlace pegado o recibido desde el menú **Compartir** del navegador, la aplicación descarga la página por HTTPS, extrae en el propio dispositivo el texto principal del artículo, lo segmenta en oraciones y lo lee en voz alta con el motor `TextToSpeech` del sistema, ofreciendo controles accesibles de pausa/reanudación, ajuste de velocidad ($0.75\times$ a $2.5\times$) y navegación oración por oración.

---

## 2. Equipo y roles Scrum

| Integrante | Rol en Scrum | Responsabilidades principales |
|---|---|---|
| **Ángel Valentín Rodríguez Gómez** | Product Owner | Definición del alcance, priorización del backlog, comunicación del equipo y validación de criterios de aceptación. |
| **Eduardo de Jesús Tafoya Espinosa** | Scrum Master | Facilitación del flujo de trabajo, seguimiento de entregables por sprint, gestión de impedimentos y control de calidad. |
| **Ambos integrantes** | Equipo de desarrollo | Análisis, diseño UX/UI en Figma, implementación en Kotlin/Jetpack Compose y ejecución de pruebas con TalkBack. |

---

## 3. Calendario de sprints (MiniHackatón 2026)

| Sprint | Fechas | Objetivo y entregable comprometido | Estado |
|---|---|---|---|
| **Sprint 0** | Mié. 28/oct/2026 | Inscripción formal, definición del alcance comprometido, creación del repositorio y backlog inicial. | En curso / Preparado |
| **Sprint 1** | Jue. 29/oct/2026 | Documentación técnica: SRS, RAD, justificación, alcance, metodología, diagramas UML, estudio de corpus web y casos de prueba. | Preparado |
| **Sprint 2** | Vie. 30/oct a lun. 02/nov/2026 | Diseño UX/UI en Figma, desarrollo del prototipo móvil ejecutable (V1) y pruebas de los flujos críticos. | Pendiente de inicio |
| **Sprint 3** | Mar. 03/nov a jue. 05/nov/2026 | Versión funcional evaluable, video de demostración, pulido final y defensa técnica. | Pendiente de inicio |

---

## 4. Alcance comprometido

### Versión evaluable (V1)
- **RF-01 · Captura de artículo por URL:** pegar un enlace o recibirlo mediante el menú *Compartir* (`ACTION_SEND`) del navegador, validando el esquema HTTPS y respetando `robots.txt` (RFC 9309).
- **RF-02 · Extracción local en capas:**
  - *Capa 1:* metadatos estructurados (`JSON-LD` / `schema.org`).
  - *Capa 2:* limpieza del DOM y extracción del cuerpo con `Jsoup` + `Readability4J` (port en Kotlin de Mozilla Readability), incluyendo la conversión de tablas HTML (`<table>`) en listas secuenciales por fila.
- **RF-03 · Síntesis de voz local:** lectura en voz alta con el motor `TextToSpeech` de Android, detección automática del idioma del artículo (`lang` / `inLanguage`) e inicio del primer audio en menos de 3 segundos.
- **RF-04 · Controles de reproducción:** pausar, reanudar y ajustar la velocidad de lectura entre $0.75\times$ y $2.5\times$, integrados con `AndroidX Media3` (`MediaSessionService`) para operar desde auriculares y pantalla bloqueada.
- **RF-05 · Navegación por oraciones:** segmentación del texto con `BreakIterator` (con reglas para abreviaturas en español), generación de marcado SSML y salto directo a la oración anterior, siguiente o a cualquier frase seleccionada.
- **RF-06 · Interfaz accesible (WCAG 2.2 AA):** anuncios de estado para TalkBack (`liveRegion`), botones de al menos $48 \times 48\text{ dp}$, alto contraste y acciones personalizadas de accesibilidad.

### Exclusiones documentadas (Fase 2)
- Clasificador ligero de bloques con `ONNX Runtime` (Capa 3).
- Resumen breve local con procesamiento de lenguaje natural (`ONNX` / `MediaPipe`).
- Guardado de artículos y caché de audio para lectura sin conexión (`RepositorioOffline`).
- Voces neuronales locales (`Piper` / `Kokoro`), comandos por voz y extensión para navegador de escritorio.

---

## 5. Estructura de la documentación preparatoria

- [`docs/BACKLOG.md`](docs/BACKLOG.md) — Product Backlog (V1 y Fase 2), Historias de Usuario, criterios de aceptación, estimaciones, desglose por sprint y Definición de Terminado (DoD).
- [`docs/ESTUDIO_SITIOS_CORPUS.md`](docs/ESTUDIO_SITIOS_CORPUS.md) — Investigación técnica de 20 sitios web reales (17 aptos para el corpus de prueba y 3 descartados por bloqueo/CAPTCHA) para validar la meta del 90 % de exactitud en extracción.
- [`docs/CASOS_DE_PRUEBA.md`](docs/CASOS_DE_PRUEBA.md) — Especificación de casos de prueba para los tres flujos críticos, manejo de errores y lista de verificación de accesibilidad con TalkBack.

---

## 6. Declaración de herramientas, bibliotecas y estándares (DoD 3.5)

Conforme a la sección 3.5 del RAD (*Definición de terminado*), se declaran las bibliotecas, estándares y herramientas previstas para el proyecto:

- **Plataforma e interfaz:** Kotlin, Android SDK (`minSdk 26`, `targetSdk 36`), Jetpack Compose (Material 3), AndroidX Navigation Compose y DataStore Preferences.
- **Extracción y red:** `OkHttp` (descarga HTTPS), `Jsoup` (manipulación del DOM y conversión de tablas), `Readability4J` (algoritmo de extracción basado en Mozilla Readability) y `kotlinx-serialization-json` (lectura de bloques `JSON-LD`).
- **Voz y reproducción multimedia:** `android.speech.tts.TextToSpeech` (motor del sistema), `java.text.BreakIterator` (segmentación de oraciones) y `AndroidX Media3` (`media3-session`, `media3-common`).
- **Pruebas:** `JUnit 4` con corpus local de páginas HTML reales.
- **Estándares de referencia:** WCAG 2.2 Nivel AA, WAI-ARIA 1.2, SSML 1.1, JSON-LD 1.1 (`schema.org`) y RFC 9309 (`robots.txt`).
- **Herramientas de apoyo:** Android Studio, Figma (diseño UX/UI) y asistencia de inteligencia artificial (Antigravity) para la planeación técnica, estructuración del backlog, investigación del corpus de sitios y revisión de casos de prueba.
