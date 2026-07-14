# Especificación Técnica (SPEC.md) - App de Chat con IA

Este documento detalla las especificaciones técnicas, requerimientos de comportamiento, arquitectura y stack tecnológico para el desarrollo de la aplicación móvil de chat con Inteligencia Artificial.

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