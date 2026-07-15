# Especificación Técnica (SPEC.md) - App de Chat con IA

Este documento detalla las especificaciones técnicas, requerimientos de comportamiento, arquitectura y organización de la interfaz de usuario para el desarrollo de la aplicación móvil de chat con Inteligencia Artificial.

---

## 1. Requerimientos de la Aplicación

### 1.1. Comportamiento y Funcionalidades
*   **Pantalla Principal (Chat):**
    *   Interfaz para iniciar y mantener conversaciones con la IA.
    *   Flujo síncrono visual: por cada mensaje enviado por el usuario, la IA debe devolver una respuesta en pantalla.
*   **Contexto de la Conversación:**
    *   La IA debe recibir siempre como contexto histórico toda la conversación previa. Esto asegura que la generación de la nueva respuesta sea coherente con el hilo actual.
*   **Historial de Conversaciones:**
    *   Implementación de un menú lateral (Navigation Drawer) desplegable.
    *   Permite visualizar y seleccionar conversaciones previas almacenadas.
    *   En la parte superior del menú lateral, se debe incluir un botón de acción rápida para "Iniciar nueva conversación".

---

## 2. Tecnologías (Tech Stack)

Para garantizar la mantenibilidad y un rendimiento óptimo, se utilizarán las siguientes tecnologías:

*   **UI & Diseño:**
    *   **Jetpack Compose:** Sistema moderno de interfaces declarativas nativas para Android.
    *   **Material 3:** Sistema de diseño guía para componentes visuales, tipografía y colores.
*   **Lenguaje:**
    *   **Kotlin:** Lenguaje principal de desarrollo.
*   **Arquitectura y Comunicación:**
    *   **ViewModels (Architecture Components):** Encargados de gestionar el estado de la UI y la comunicación bidireccional entre la vista y la capa de datos.
    *   **Hilt:** Framework para la inyección de dependencias (Dependency Injection).
*   **Persistencia de Datos:**
    *   **Room Database:** Para el guardado y recuperación local de las conversaciones previas (historial).
    *   *Nota de desarrollo:* Es mandatorio utilizar **KSP (Kotlin Symbol Processing)** en lugar de KAPT para los procesadores y compiladores de Room que generan código.
*   **Inteligencia Artificial:**
    *   **API de OpenAI:** Servicio backend para el procesamiento del modelo de lenguaje.
    *   **open-ai kotlin (de Aallam):** Librería cliente de la comunidad de código abierto para realizar la conexión nativa con OpenAI.  
        *Repositorio:* `https://github.com/Aallam/openai-kotlin`

---

## 3. Arquitectura del Software

Se implementará una arquitectura limpia y desacoplada basada en los siguientes componentes:

*   **Capa de Presentación (UI):** Desarrollada íntegramente en Jetpack Compose.
*   **Patrón de Presentación:** Comunicación con la capa de datos mediante el patrón **MVVM** (Model-View-ViewModel).
*   **Capa de Datos:**
    *   Estructurada mediante el **Patrón Repositorio** (Repository Pattern).
    *   Los repositorios serán los encargados de abstraer y ocultar a la UI qué librerías concretas (Room, OpenAI SDK, etc.) se están utilizando para obtener o persistir la información.

---

## 4. Estructura de Pantallas y Capa UI

Cada pantalla de la aplicación debe seguir una estructura de paquetes por funcionalidad (Package-by-Feature) y respetar el patrón de desacoplamiento de Compose:

```text
ui/
└── <nombre_pantalla>/
    ├── <NombrePantalla>Route.kt         # Conector con estado (ViewModel, Navegación)
    ├── <NombrePantalla>Screen.kt        # Pantalla "Stateless" (Scaffold y estructura visual)
    ├── <NombrePantalla>ViewModel.kt     # ViewModel de la pantalla
    ├── <NombrePantalla>UiState.kt       # Estado de la UI (Representación de datos/cargas)
    └── components/                      # Composables específicos de esta pantalla
        ├── <NombrePantalla>Header.kt    # Barras superiores, títulos
        ├── <NombrePantalla>Content.kt   # Listados, formularios, contenido principal
        ├── <NombrePantalla>Item.kt      # Tarjetas o filas individuales
        ├── <NombrePantalla>Empty.kt     # Pantallas de error o estado vacío
        └── <NombrePantalla>Footer.kt    # Barra inferior, inputs, etc.
```

**Reglas de organización y arquitectura UI:**

*   **Patrón Route/Screen:**
    *   El archivo `Route.kt` es el único que tiene acceso al `ViewModel`. Se encarga de recolectar el estado y de manejar los eventos de navegación hacia otras pantallas.
    *   El archivo `Screen.kt` debe ser **completamente stateless** (sin estado propio del ViewModel). Solo recibe parámetros primitivos, el `UiState` y funciones lambda (`() -> Unit`) para los eventos. Esto garantiza que sea 100% compatible con el `@Preview` de Android Studio.
*   **Gestión de Estado (`UiState`):** Toda pantalla compleja debe representar su estado mediante una clase o interfaz sellada (`sealed interface`) llamada `<NombrePantalla>UiState.kt` que represente estados como `Loading`, `Success` y `Error`.
*   **Componentes en `components/`:**
    *   Cada componente debe ser independiente y reutilizable dentro de la pantalla. No deben acceder a variables globales ni al ViewModel.
    *   Si los componentes son muy pequeños (menos de 20 líneas), se pueden agrupar en un único archivo dentro de `components/` (ej: `ChatList.kt` puede albergar la lista y el ítem del chat si no son complejos).
*   **Pragmatismo:** Si una pantalla es estática o muy simple, se permite omitir la carpeta `components/` y definir los sub-composables privados dentro del archivo `Screen.kt` para evitar sobreingeniería.

---

## 5. Reglas Extras

*   **Verificación de compilación:** Siempre que termines de generar un código, compílalo inmediatamente para verificar que no hay ningún problema de sintaxis o dependencias rotas antes de continuar, utiliza `compileDebugKotlin`.
*   **Respeto a Gradle y Configuración de Dependencias:** Aunque pienses que los `build.gradle.kts` están incorrectos, los que tienes ahora mismo en contexto son válidos. Si tienes que modificar el `libs.versions.toml` o los ficheros gradle, simplemente añade lo nuevo que necesites, y no modifiques lo que ya existe.