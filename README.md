# App de Chat con IA (Android)

Este es un cliente móvil nativo para Android que permite interactuar con la API de OpenAI, ofreciendo una experiencia de chat fluida con persistencia de historial local.

---

## 🚀 Requisitos Previos

Antes de comenzar, asegúrate de cumplir con los siguientes requisitos en tu entorno de desarrollo:

*   **Android Studio** Ladybug (o superior).
*   **JDK 17** o superior.
*   Una **API Key de OpenAI** válida.

---

## 🛠️ Tecnologías Clave

*   **Jetpack Compose** & **Material 3** (UI)
*   **Kotlin** con Corrutinas y Flow.
*   **MVVM** (Model-View-ViewModel).
*   **Hilt** (Inyección de dependencias).
*   **Room (con KSP)** para base de datos local.
*   **openai-kotlin (Aallam)** para la integración con IA.

*Para detalles profundos sobre la arquitectura y la selección de tecnologías, por favor consulta el archivo [SPEC.md](./SPEC.md).*

---

## ⚙️ Configuración del Proyecto

### 1. Clonar el repositorio
```bash
git clone [https://github.com/tu-usuario/nombre-del-repositorio.git](https://github.com/tu-usuario/nombre-del-repositorio.git)
cd nombre-del-repositorio
```

### 2. Configurar la API Key de OpenAI
Para que la aplicación pueda comunicarse con la IA, debes proveer tu API Key.
Crea un archivo llamado `local.properties` en la raíz de tu proyecto (si no existe ya) y añade la siguiente variable:

```properties
OPENAI_API_KEY="tu_api_key_aqui"
```

### 3. Compilar y Ejecutar
1. Abre el proyecto en Android Studio.
2. Deja que Gradle sincronice las dependencias (`Sync Project with Gradle Files`).
3. Conecta un dispositivo físico o inicia un emulador Android.
4. Presiona el botón **Run (Shift + F10)**.

---

## 📂 Estructura del Código

```text
app/
├── src/main/java/com/tu/paquete/
│   ├── data/          # Repositorios, Base de datos (Room) y API de OpenAI
│   ├── di/            # Módulos de inyección de dependencias con Hilt
│   ├── ui/            # Vistas en Compose y ViewModels (MVVM)
│   └── MainActivity.kt
└── build.gradle.kts
```

---

## 🤝 Contribuciones

Si deseas colaborar con el proyecto:
1. Lee detenidamente el [SPEC.md](./SPEC.md) para comprender las reglas de arquitectura.
2. Crea una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`).
3. Realiza tus commits respetando el patrón arquitectónico MVVM.
4. Abre un Pull Request describiendo detalladamente tus cambios.

---

## 📄 Licencia

Este proyecto se distribuye bajo la licencia MIT.