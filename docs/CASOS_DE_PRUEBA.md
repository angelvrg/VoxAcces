# Especificación de Casos de Prueba — VoxAccess V1

Documento de preparación (**Sprint 1**) que define los casos de prueba para los **tres flujos críticos**, el manejo accesible de errores y la verificación de **WCAG 2.2 Nivel AA con TalkBack**, conforme a las secciones 2.4, 3.5 y 6 del RAD.

---

## 1. Flujo Crítico 1 — Captura del artículo por URL (UC1 / RF-01)

| ID | Caso de prueba | Precondiciones | Pasos de ejecución | Resultado esperado |
|---|---|---|---|---|
| **CP-01** | Pegar una URL válida desde la pantalla de inicio | App abierta en `InicioScreen`; TalkBack activo; URL válida de `bbc.com/mundo` en el portapapeles. | 1. Tocar dos veces el botón *"Pegar del portapapeles"*.<br>2. Tocar dos veces el botón *"Leer artículo"*. | El campo se llena con la URL; TalkBack anuncia *"Extrayendo artículo principal…"* y abre la pantalla de lectura. |
| **CP-02** | Recibir un artículo desde el menú *Compartir* del navegador | Artículo abierto en Chrome para Android; TalkBack activo. | 1. Abrir el menú *Compartir* en Chrome (que envía título + URL).<br>2. Seleccionar **VoxAccess**. | `ValidadorUrl` extrae la URL limpia del texto compartido, abre directamente la pantalla de lectura e inicia la extracción sin pasos intermedios. |
| **CP-03** | Normalización automática de `http://` a `https://` | Usuario introduce un enlace con esquema `http://`. | 1. Ingresar `http://es.wikipedia.org/wiki/P%C3%A9njamo`.<br>2. Activar *"Leer artículo"*. | El validador convierte el enlace a `https://...` antes de realizar la petición de red. |
| **CP-04** | Rechazo accesible de URL inválida o esquema inseguro | App en `InicioScreen`; TalkBack activo. | 1. Ingresar `texto sin enlace` o `javascript:alert(1)` o `http://192.168.1.1`.<br>2. Activar *"Leer artículo"*. | No se realiza ninguna petición de red; TalkBack anuncia: *"El enlace ingresado no es válido. Ingresa una dirección web pública con HTTPS."* |
| **CP-05** | Respeto de bloqueo en `robots.txt` (RFC 9309) | Ruta prohibida por `Disallow:` para `User-agent: *` o `VoxAccess`. | 1. Solicitar la lectura de una URL cuya ruta esté marcada con `Disallow`. | El descargador cancela la descarga del artículo y TalkBack anuncia que el sitio no permite la lectura automática según su archivo `robots.txt`. |

---

## 2. Flujo Crítico 2 — Extracción en capas y lectura en voz alta (UC2 / RF-02, RF-03)

| ID | Caso de prueba | Precondiciones | Pasos de ejecución | Resultado esperado |
|---|---|---|---|---|
| **CP-06** | Extracción por Capa 1 (`JSON-LD` con `articleBody`) | Artículo de *El País*, *Xataka* o *Agencia SINC*. | 1. Abrir la URL en VoxAccess.<br>2. Escuchar las primeras 5 oraciones y revisar la lista en pantalla. | Se extrae el título, autor y cuerpo completo desde `JSON-LD` sin incluir menús, banners de cookies ni tarjetas de *"Artículos relacionados"*. |
| **CP-07** | Extracción por Capa 2 (`Readability4J` + `Jsoup`) cuando no hay `articleBody` | Artículo de *BBC Mundo*, *Gob.mx SRE* o *Animal Político*. | 1. Abrir la URL en VoxAccess.<br>2. Verificar el contenido extraído. | `Readability4J` aísla el cuerpo principal y descarta encabezados, barras laterales y pies de página. |
| **CP-08** | Conversión accesible de tablas HTML (`<table>`) | Artículo `wikipedia_es_penjamo` o `wikipedia_en_screen_reader`. | 1. Abrir el artículo.<br>2. Navegar hasta la sección de la tabla. | `ConversorTablasAccesibles` transforma cada fila en una oración secuencial del tipo *"Fila 1: [Encabezado], [Valor]..."* legible por voz. |
| **CP-09** | Tiempo hasta el primer audio menor a 3 segundos | Conexión Wi-Fi estable; motor TTS de Android inicializado al abrir la app. | 1. Cronometrar desde que termina la descarga HTML hasta que el altavoz emite la primera palabra. | TalkBack anuncia *"Artículo listo"* y la síntesis de la primera oración comienza en **$< 3\text{ segundos}$**. |
| **CP-10** | Detección automática del idioma del artículo (`es` / `en`) | Voces en español e inglés disponibles en el motor TTS del teléfono. | 1. Abrir un artículo en español (`sinc_anatomia_lenguas`).<br>2. Abrir después un artículo en inglés (`npr_psychedelics_inflammation`). | `ControladorTTS` lee el atributo `lang` / `inLanguage` y cambia automáticamente la voz del sintetizador al idioma correspondiente. |
| **CP-11** | Segmentación correcta de abreviaturas y cifras en español | Texto con `Dr.`, `Sra.`, `EE. UU.`, `pág. 14` y `3.14`. | 1. Procesar el texto con `SegmentadorOraciones`. | Ninguna oración se corta de forma prematura en el punto de una abreviatura ni en un punto decimal; cada `Oracion` incluye su `marcadoSSML` bien formado. |

---

## 3. Flujo Crítico 3 — Control de reproducción y navegación por oraciones (UC3 / RF-04, RF-05)

| ID | Caso de prueba | Precondiciones | Pasos de ejecución | Resultado esperado |
|---|---|---|---|---|
| **CP-12** | Pausar y reanudar desde la interfaz | Artículo en reproducción en `LectorScreen`. | 1. Activar el botón *"Pausar"*.<br>2. Activar el botón *"Reanudar"*. | La voz se detiene de inmediato al pausar y retoma desde el inicio de la oración activa al reanudar. |
| **CP-13** | Ajuste de velocidad de lectura ($0.75\times$ a $2.5\times$) | Artículo en reproducción. | 1. Tocar *"Aumentar velocidad"* hasta llegar a $2.5\times$.<br>2. Tocar *"Disminuir velocidad"* hasta llegar a $0.75\times$. | La tasa de habla de `TextToSpeech` cambia de inmediato en pasos de $0.25\times$, se anuncia el nuevo valor y no permite salir del rango $[0.75\times, 2.5\times]$. |
| **CP-14** | Saltar a la oración siguiente y anterior | Artículo en reproducción en la oración $k$ ($k > 1$). | 1. Activar *"Oración siguiente"*.<br>2. Activar dos veces *"Oración anterior"*. | El índice pasa a $k+1$ y luego a $k-1$; la lista resalta la oración activa y la voz pronuncia de inmediato la nueva oración. |
| **CP-15** | Volver a una frase exacta tocándola en la lista | Artículo cargado con múltiples oraciones. | 1. Recorrer la lista de oraciones con TalkBack.<br>2. Hacer doble toque sobre la oración #8. | La reproducción salta directamente a la oración #8 y continúa desde ahí. |
| **CP-16** | Control desde auriculares y pantalla bloqueada (`Media3`) | Artículo en reproducción; pantalla bloqueada o auriculares conectados. | 1. Presionar el botón *Play/Pause* de los auriculares o de la notificación en pantalla bloqueada.<br>2. Presionar *Siguiente pista* / *Pista anterior*. | `ServicioLectura` (`MediaSessionService`) pausa/reanuda la lectura y avanza/retrocede una oración sin necesidad de desbloquear el teléfono. |
| **CP-17** | Pausa automática por llamada o desconexión de auriculares | Artículo en reproducción con auriculares. | 1. Desconectar los auriculares (o simular pérdida de foco de audio). | La reproducción se pausa de inmediato para no continuar en altavoz sin consentimiento. |

---

## 4. Manejo accesible de errores de red y extracción

| ID | Escenario de error | Causa | Resultado esperado con TalkBack |
|---|---|---|---|
| **CP-18** | Dispositivo sin conexión a internet | Modo avión activado al intentar abrir una URL. | Se anuncia: *"No hay conexión a internet. Verifica tu red Wi-Fi o datos móviles e inténtalo de nuevo."* |
| **CP-19** | Sitio con bloqueo anti-bot o muro de pago (`HTTP 403` / `401`) | URL de un sitio como *La Jornada* o *Medium* que devuelve reto Cloudflare. | No intenta evadir el bloqueo; anuncia: *"El sitio web restringe la descarga automática de este artículo."* |
| **CP-20** | Página sin cuerpo de artículo (portada o página vacía por JS) | URL de una página con menos del mínimo de palabras de cuerpo. | `ExtractorTresCapas` lanza `ExtraccionFallidaException` y la UI anuncia: *"No se encontró un artículo legible en este enlace."* |

---

## 5. Lista de verificación de Accesibilidad (WCAG 2.2 Nivel AA + TalkBack)

Cada entrega del **Sprint 2** y **Sprint 3** se valida en un teléfono Android real verificando:

- [ ] **1.4.3 Contraste mínimo (AA):** relación de contraste $\ge 4.5:1$ en todo el texto normal y $\ge 3:1$ en texto grande e íconos de control.
- [ ] **1.4.4 Cambio de tamaño del texto (AA):** toda la tipografía usa unidades `sp` y la interfaz sigue siendo operable sin recortes con el tamaño de fuente del sistema al $200\%$.
- [ ] **2.4.6 Encabezados y etiquetas (AA):** los títulos de pantalla y el título del artículo están marcados con `semantics { heading() }` para permitir navegación por encabezados en TalkBack.
- [ ] **2.5.8 Tamaño del objetivo (AA — WCAG 2.2):** todos los botones interactivos tienen un área táctil mínima de $48 \times 48\text{ dp}$ (los controles principales de reproducción usan $\ge 64\text{ dp}$).
- [ ] **4.1.2 Nombre, función, valor (A):** cada botón e interruptor expone su `contentDescription` y estado actualizado (p. ej. *"Pausar lectura"* vs. *"Reanudar lectura"*, *"Velocidad actual: 1.25 por"*).
- [ ] **4.1.3 Mensajes de estado (AA):** los avisos *"Extrayendo artículo principal…"*, *"Artículo listo"* y los errores se emiten mediante `liveRegion = LiveRegionMode.Polite`.
- [ ] **No colisión de voces (TalkBack vs. TTS de lectura):** durante la reproducción del artículo, el cambio automático de oración no fuerza el movimiento del foco de accesibilidad de TalkBack, evitando que ambas voces hablen al mismo tiempo.
- [ ] **Acciones personalizadas (`CustomAccessibilityAction`):** desde cualquier punto del lector, el menú de acciones de TalkBack ofrece atajos directos para *Oración siguiente*, *Oración anterior*, *Aumentar velocidad* y *Disminuir velocidad*.
