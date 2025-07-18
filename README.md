# NewEyes 👁️📱

**NewEyes** es una aplicación Android nativa desarrollada en **Kotlin**, diseñada para asistir a personas con discapacidad visual. Integra tecnologías de procesamiento de audio e imagen, APIs de IA generativa y un backend propio para brindar navegación asistida, análisis del entorno y detección de emociones en tiempo real.

---

## 🎯 Objetivo

El propósito de NewEyes es proporcionar un **asistente inteligente** que ayude a personas invidentes a:
- Navegar por su entorno físico usando la cámara del móvil.
- Obtener descripciones habladas del entorno gracias a IA generativa.
---

## ⚙️ Tecnologías utilizadas

### 👨‍💻 Frontend
- Android nativo con **Kotlin**
- **ViewBinding** para UI segura y eficiente
- **Retrofit** para comunicación HTTP
- **TextToSpeech** para salida de voz
- Permisos de cámara y almacenamiento gestionados

### 🤖 Backend
- **FastAPI** en Python para recibir imágenes y datos
- Integración con:
  - **Groq** para procesamiento de voz e imagen
  - **Gemini API** (Google) para descripción de entorno
    
---

## 🧪 Funcionalidades principales

- 🎙 **Asistencia por Voz**: Interacción por comandos de voz.
- 📸 **Captura Inteligente**: Toma imágenes automáticamente y las describe o analiza.
- 🧭 **Navegación Asistida**: Usa la cámara para detectar objetos o leer textos del entorno.
- 🌐 **Comunicación en Tiempo Real** con rooms de SocketIO.

---

## 🔑 Configuración de claves

El repositorio no incluye claves ni keystore. Antes de compilar, reemplaza estos placeholders:

- `app/src/main/java/com/neweyes/chat/groq/GroqApi.kt`: `YOUR_GROQ_API_KEY` → tu API key de Groq.
- `app/src/main/AndroidManifest.xml`: `YOUR_GOOGLE_MAPS_API_KEY` → tu API key de Google Maps.
- `gradle.properties`: `CHANGE_ME` en `RELEASE_STORE_PASSWORD` y `RELEASE_KEY_PASSWORD` → las contraseñas de tu keystore.
- Genera tu propio keystore con `keytool`, usando el archivo y alias de `RELEASE_STORE_FILE` y `RELEASE_KEY_ALIAS`; `*.jks` y `*.keystore` están en `.gitignore`.
