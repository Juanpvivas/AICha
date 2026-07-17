# ARCHITECTURE.md — Arquitectura Android (módulo único)

## 0. Estado y alcance de este documento

Este documento define la arquitectura transversal del proyecto **AICha** (app de chat con IA para Android). A diferencia de una propuesta genérica multi-módulo, esta versión está ajustada a la realidad del proyecto: **un único módulo de Gradle (`app`)**, organizado por paquetes siguiendo el patrón **package-by-feature**.

Este documento cubre la arquitectura transversal (capas, paquetes, DI, concurrencia, convención de UI, testing, CI/CD). El detalle de comportamiento y requerimientos de una feature concreta (por ejemplo, la feature de Chat) vive en su propio documento de spec (ver `SPEC.md`), no aquí.

---

## 1. Principios de arquitectura

- **Separación por capas**: presentación, datos (y dominio cuando la lógica lo justifique), con dependencias apuntando siempre hacia adentro.
- **Unidireccionalidad de datos (UDF)**: la UI emite eventos, el ViewModel procesa y expone estado inmutable; la UI solo renderiza estado, nunca lo muta directamente.
- **Single source of truth**: cada dato tiene un único origen autoritativo (el repositorio correspondiente); la UI nunca guarda copias divergentes del dato.
- **Testabilidad por diseño**: la lógica de negocio vive en clases sin dependencias de Android (`ViewModel`, y `UseCase` cuando se justifique) para poder testearse con JUnit puro.
- **Disciplina de paquetes en vez de imposición de Gradle**: al ser un módulo único, los límites entre capas no se imponen por el compilador (como sí ocurre en multi-módulo), sino por convención de equipo. Esto exige más disciplina en code review para no romper la separación.
- **Consistencia de paradigma**: coroutines + Flow en toda la app; no mezclar con RxJava ni LiveData.

---

## 2. Stack tecnológico

| Área | Elección | Nota |
|---|---|---|
| Lenguaje | Kotlin | — |
| UI | Jetpack Compose + Material 3 | — |
| Arquitectura de presentación | MVVM (`ViewModel` + `StateFlow`) | — |
| Inyección de dependencias | Hilt | — |
| Concurrencia | Coroutines + Flow | — |
| Navegación | Compose Navigation | — |
| Proveedor de IA | **Groq** (motor de inferencia) | El cliente HTTP es el propio SDK `openai-kotlin` (Aallam), que trae su transporte (Ktor) — no se usa Retrofit para esta integración. |
| Persistencia local | Room (historial de conversaciones) + KSP (no KAPT) | — |
| Testing unitario | JUnit + MockK + Turbine (para Flow) | — |
| Logging | Timber (opcional) | A confirmar si se adopta |

> Si en el futuro se agregan integraciones REST propias (backend propio, analytics, etc.) que no sean el cliente de Groq, ahí sí aplica evaluar Retrofit + OkHttp como cliente HTTP genérico, ya que el SDK de `openai-kotlin` es específico para la integración con Groq/OpenAI.

---

## 3. Organización de paquetes (módulo único, package-by-feature)

```text
com.juanpvivas.aichatjp/
├── data/
│   ├── local/               # Room: entities, DAOs, instancia de base de datos, migraciones
│   ├── remote/               # Cliente de Groq (openai-kotlin), request/response mapeados si aplica
│   ├── repository/           # Implementaciones de repositorio (única capa que conoce local/ y remote/)
│   └── model/                # Modelos de dominio compartidos entre repository y ui
├── di/                        # Módulos Hilt: NetworkModule, DatabaseModule, RepositoryModule
├── ui/
│   ├── <feature>/             # Un paquete por feature (ej. chat/, history/)
│   │   ├── <Feature>Route.kt
│   │   ├── <Feature>Screen.kt
│   │   ├── <Feature>ViewModel.kt
│   │   ├── <Feature>UiState.kt
│   │   └── components/
│   └── theme/                 # Colores, tipografía y tema de Material 3
└── MainActivity.kt
```

**Reglas de dependencia entre paquetes (aplicadas por convención/code review, no por el compilador):**

- `ui/<feature>/` depende de `data/repository/` (vía interfaz) y de `data/model/`. Nunca importa clases de `data/local/` (entidades de Room) ni de `data/remote/` (DTOs) directamente.
- `data/repository/` es la única capa que conoce Room y el cliente de Groq; traduce entidades/DTOs a `data/model/` antes de exponerlos.
- Si la lógica de negocio de una feature empieza a repetirse entre ViewModels o a crecer en complejidad, extraerla a `domain/usecase/` (paquete a introducir solo cuando se justifique, no por defecto desde el día uno).
- `di/` es el único paquete que puede referenciar simultáneamente `data/local`, `data/remote` y `data/repository` para armar el grafo de dependencias.

---

## 4. Capa de presentación: convención Route/Screen

Cada pantalla de la aplicación sigue esta estructura y respeta el patrón de desacoplamiento de Compose:

```text
ui/
└── <nombre_pantalla>/
    ├── <NombrePantalla>Route.kt         # Conector con estado (ViewModel, Navegación)
    ├── <NombrePantalla>Screen.kt        # Pantalla "Stateless" (Scaffold y estructura visual)
    ├── <NombrePantalla>ViewModel.kt     # ViewModel de la pantalla
    ├── <NombrePantalla>UiState.kt       # Estado de la UI (Loading, Success, Error, Empty)
    └── components/                      # Composables específicos de esta pantalla
        ├── <NombrePantalla>Header.kt
        ├── <NombrePantalla>Content.kt
        ├── <NombrePantalla>Item.kt
        ├── <NombrePantalla>Empty.kt
        └── <NombrePantalla>Footer.kt
```

**Reglas:**

- **Patrón Route/Screen**: `Route.kt` es el único archivo con acceso al `ViewModel`; recolecta el estado y maneja eventos de navegación hacia otras pantallas. `Screen.kt` es **completamente stateless**: solo recibe parámetros primitivos, el `UiState` y lambdas (`() -> Unit`) para eventos, de forma que sea 100% compatible con `@Preview`.
- **`UiState`**: toda pantalla con datos asíncronos modela su estado como `sealed interface` (`Loading`, `Success`, `Error`, y `Empty` cuando aplique).
- **Componentes en `components/`**: independientes y reutilizables dentro de la pantalla; no acceden al ViewModel ni a variables globales. Componentes muy pequeños (<20 líneas) pueden agruparse en un único archivo (ej. `ChatList.kt` puede contener lista e ítem si no son complejos).
- **Pragmatismo**: en pantallas estáticas o muy simples se permite omitir `components/` y definir sub-composables privados dentro de `Screen.kt`.
- **Localización**: prohibido hardcodear textos en los Composables. Todo texto visible va en `strings.xml` y se llama con `stringResource(R.string.identificador)`.

---

## 5. Manejo de estado y concurrencia

- Todo `Flow` expuesto desde un ViewModel usa `stateIn(scope, SharingStarted.WhileSubscribed(5_000), initialValue)` para sobrevivir cambios de configuración sin fugas.
- Los `Dispatchers` se inyectan (nunca `Dispatchers.IO` hardcodeado dentro de una clase) para poder sustituirlos en tests.
- Efectos secundarios de un solo disparo (navegación, snackbars) se exponen como `Channel`/`SharedFlow`, nunca como parte del `UiState` persistente, para que no se repitan en recomposición o rotación.

---

## 6. Inyección de dependencias (Hilt)

- Un `@Module` por tipo de binding dentro de `di/` (`NetworkModule` para el cliente de Groq, `DatabaseModule` para Room, `RepositoryModule` para los bindings de repositorio).
- Repositorios expuestos vía interfaz (`@Binds`), nunca la implementación concreta, para permitir fakes en tests.
- `@Singleton` para el cliente de Groq y la instancia de Room; `@ViewModelScoped` para dependencias atadas al ciclo de vida de una pantalla.
- `hiltViewModel()` en Compose para inyectar ViewModels en el árbol de navegación.

---

## 7. Persistencia local

- **Room** para el historial de conversaciones, con **KSP** (no KAPT) para el procesador de anotaciones.
- Migraciones obligatorias y testeadas ante cualquier cambio de esquema; no usar `fallbackToDestructiveMigration()` en producción.
- El repositorio correspondiente decide cuándo leer de Room vs. cuándo pedir una nueva respuesta a Groq; la UI nunca accede a los DAOs directamente.

---

## 8. Integración con Groq

- El cliente de IA vive en `data/remote/`, usando el SDK `openai-kotlin` apuntando al endpoint compatible de Groq.
- El repositorio de chat es responsable de construir el contexto histórico completo de la conversación antes de cada llamada (ver `SPEC.md` para el detalle de comportamiento de esta feature).
- Errores de red/API (timeout, rate limit, error de modelo) se normalizan en un tipo de error propio antes de llegar al ViewModel; el ViewModel decide cómo se traduce a `UiState.Error`.
- La API Key de Groq nunca se hardcodea: se lee desde `local.properties`/variables de entorno y se inyecta vía `BuildConfig` o Hilt, nunca se sube al repositorio.

---

## 9. Testing

| Capa | Herramientas | Qué se cubre |
|---|---|---|
| Repositorio | JUnit + MockK + fixtures | Mapeo de datos, manejo de errores de red, lectura/escritura en Room (in-memory) |
| ViewModel | JUnit + MockK + Turbine | Transiciones de `UiState`: un test por estado (loading/success/error/empty) |
| UI | Compose Testing | Estados principales renderizados correctamente, interacciones clave |

- Los fakes/mocks compartidos entre features se agrupan en un paquete de testing común (ej. `testutil/` o `data/repository/fake/`) para no duplicarlos.

---

## 10. Convenciones de código y calidad

- **Linting**: ktlint + detekt en pre-commit/CI, bloqueando merge si fallan.
- **Naming**: `PascalCase` para clases/composables, `camelCase` para funciones/variables, sufijos consistentes (`Repository`, `ViewModel`, `UiState`, `Screen`, `Route`).
- **Verificación de compilación**: antes de dar por cerrado cualquier cambio de código, compilar con `./gradlew compileDebugKotlin` para detectar errores de sintaxis o dependencias rotas.
- **Gradle**: no modificar configuración existente en `build.gradle.kts`/`libs.versions.toml` salvo que sea estrictamente necesario; agregar lo nuevo sin tocar lo ya definido salvo justificación explícita.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`).
- **Branching**: `feature/<nombre-descriptivo>` desde `main`; prohibido commitear directo a `main`. Todo cambio pasa por Pull Request.

---

## 11. CI/CD

Pipeline mínimo sugerido (a adaptar al proveedor real):

1. **Lint**: ktlint + detekt.
2. **Build**: `./gradlew assembleDebug`.
3. **Test**: `./gradlew test` (suite unitaria completa en cada PR).
4. **Artefactos**: generación de APK en merges a `main` (opcional, según necesidad del proyecto).

---

## 12. Seguridad

- La API Key de Groq nunca se sube al repositorio: vive en `local.properties` (ignorado por git) o en variables de entorno de CI.
- R8/minificación habilitado en builds de release.

---

## 13. Puntos abiertos

- [ ] Estrategia de expiración/límite del historial de conversaciones (¿se guarda indefinidamente? ¿hay límite de mensajes o de conversaciones?).
- [ ] Comportamiento ante error de la API de Groq a mitad de una respuesta en streaming (si se implementa streaming a futuro).
- [ ] Si el proyecto crece y se justifica, evaluar extracción a multi-módulo Gradle (`core`, `feature-chat`, etc.) — no aplica por ahora dado el tamaño del proyecto.