# VoxAccess

**Lector web accesible para Android** — Escucha artículos de noticias, ciencia y contenido educativo sin distracciones.

VoxAccess complementa a los lectores de pantalla como **TalkBack**. Pegás un enlace (o lo compartís desde el navegador) y la app extrae el texto principal del artículo, lo limpia de menús, anuncios y elementos irrelevantes, y lo lee en voz alta con controles pensados para accesibilidad.

Todo el procesamiento ocurre **en tu dispositivo**. No se envían datos a servidores externos ni se requiere cuenta.

## Descargar

**Versión actual:** [Descargar APK](https://github.com/angelvrg/VoxAcces/releases/download/VoxAccess/app-release.apk)

> Requiere Android 8.0 o superior.  
> Si Android muestra una advertencia de “origen desconocido”, activá la opción de instalar apps de fuentes externas solo para este archivo.

## Características principales

- **Captura sencilla** — Pegá una URL o usá el menú *Compartir* del navegador (Chrome, Firefox, etc.).
- **Extracción inteligente** — Identifica el título, autor y cuerpo principal del artículo (JSON-LD + algoritmo tipo Readability), convirtiendo tablas en texto secuencial comprensible.
- **Lectura por oraciones** — Segmentación precisa del texto (respeta abreviaturas en español) y navegación oración por oración.
- **Controles de reproducción** — Pausar, reanudar y ajustar velocidad entre 0.75× y 2.5×. Compatible con auriculares, notificación y pantalla de bloqueo (Media3).
- **Interfaz accesible** — Diseñada para TalkBack (WCAG 2.2 AA): botones grandes, alto contraste, anuncios de estado y acciones personalizadas.
- **Privacidad por diseño** — Descarga solo por HTTPS, respeta `robots.txt` y procesa todo localmente.

## Cómo usarla

1. Abrí VoxAccess o compartí un enlace desde el navegador hacia la app.
2. La aplicación descarga la página, extrae el contenido principal y comienza a leerlo en voz alta.
3. Usá los controles en pantalla, los botones de los auriculares o las acciones de TalkBack para:
   - Pausar / reanudar
   - Cambiar la velocidad de lectura
   - Ir a la oración anterior o siguiente
   - Saltar a cualquier oración de la lista

En **Ajustes** podés elegir la voz del sistema y la velocidad predeterminada.

## Requisitos

- Android 8.0 (API 26) o superior
- Motor Text-to-Speech del sistema (ya incluido en Android)
- Conexión a internet para descargar el artículo (el audio se genera en el dispositivo)

## Compilar desde el código fuente

```bash
git clone https://github.com/angelvrg/VoxAcces.git
cd VoxAcces
./gradlew :app:assembleDebug