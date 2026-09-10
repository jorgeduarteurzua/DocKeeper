# DocKeeper — Tus documentos, en orden

App Android **100% local y offline** para organizar documentos, fotos y enlaces.
Todo se guarda en el dispositivo (base de datos SQLite + archivos en almacenamiento
privado de la app). No hay nube, cuentas ni conexión requerida.

## Concepto

Jerarquía de 3 niveles:

```
Categoría (ej: Documentos Médicos)
  └── Registro / Item (ej: Examen de Sangre - Marzo 2026)
        ├── Adjunto: hoja1.jpg (foto)
        ├── Adjunto: hoja2.jpg (foto)
        ├── Adjunto: informe.pdf (documento)
        └── Adjunto: enlace / QR del laboratorio
```

Un registro puede tener **uno o varios adjuntos** (varias hojas de un examen, un
informe, un enlace, etc.).

## Funcionalidades

- Crear, editar y eliminar **categorías** (con color y fecha de creación).
- Conteo automático de registros por categoría y de adjuntos por registro.
- Crear **registros** con título y descripción.
- Agregar adjuntos: **subir foto(s)** de galería, **sacar foto** con la cámara,
  **subir documento(s)** (PDF, etc.), **escanear QR** y **pegar enlace**.
  La subida desde galería y de documentos permite **seleccionar uno o varios
  archivos a la vez** en un solo paso. Las fotos usan el **Photo Picker** moderno
  de Android (multiselección nativa), y los documentos el selector del sistema
  con selección múltiple.
- **Visualizar**: fotos a pantalla completa, documentos con la app del sistema,
  enlaces/QR con opción de abrir, copiar y compartir.
- **Imprimir**: un adjunto individual, o todas las fotos de un registro como un
  solo PDF multipágina (ideal para exámenes de varias hojas).
- **Buscar** por título de registro, descripción o nombre de categoría.
- **Eliminar** en los 3 niveles (borra también los archivos físicos).
- Modo claro/oscuro y **Material You** (color dinámico en Android 12+).

## Stack técnico

| Área | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) + repositorios |
| Base de datos | Room (SQLite) |
| Inyección | Hilt |
| Cámara | CameraX |
| QR / códigos | ML Kit Barcode Scanning |
| Imágenes | Coil |
| Impresión | Android PrintManager |
| Navegación | Navigation Compose |

- **minSdk**: 26 (Android 8.0) · **targetSdk / compileSdk**: 34 (Android 14)
- **Java / JVM target**: 17
- **Package**: `com.dockeeper.app`

## Estructura del proyecto

```
app/src/main/java/com/dockeeper/app/
├── DocKeeperApp.kt            # Application con Hilt
├── MainActivity.kt            # Activity única (Compose)
├── data/
│   ├── file/FileManager.kt    # Copiar/borrar archivos internos
│   ├── local/                 # Room: entidades, DAOs, DB, relaciones, converters
│   ├── mapper/                # Entidad -> modelo de dominio
│   └── repository/            # CategoryRepository, ItemRepository
├── di/DatabaseModule.kt       # Provisión Hilt de la DB y DAOs
├── domain/model/              # Modelos de dominio (Category, Item, Attachment...)
├── ui/
│   ├── categories/            # Home: lista de categorías
│   ├── categorydetail/        # Lista de registros de una categoría
│   ├── itemedit/              # Crear/editar registro + agregar adjuntos
│   ├── itemdetail/            # Galería de adjuntos + imprimir/eliminar
│   ├── viewer/                # Visor de un adjunto (foto/doc/link)
│   ├── search/                # Búsqueda global
│   ├── qr/                    # Escáner QR (CameraX + ML Kit)
│   ├── navigation/            # Rutas y NavGraph
│   ├── common/                # Componentes y utilidades UI compartidas
│   └── theme/                 # Tema Material 3 y paleta de categorías
└── util/                      # Impresión (PrintManager) y acciones externas
```

## Cómo abrir y ejecutar

Necesitas **Android Studio** (versión Koala 2024.1.1 o más reciente, que incluye
JDK 17 y el Android Gradle Plugin 8.5+).

1. Abre Android Studio → **Open** → selecciona esta carpeta (`App DocOrder`).
2. Android Studio detectará el proyecto Gradle y **descargará las dependencias**
   automáticamente (requiere conexión la primera vez).
   - Si aparece un aviso de que falta el Gradle Wrapper, Android Studio ofrecerá
     regenerarlo; acéptalo. También puedes ejecutar en la terminal integrada:
     `gradle wrapper --gradle-version 8.9` (si tienes Gradle) y luego sincronizar.
3. Conecta un dispositivo físico (con **Depuración USB** activada) o crea un
   **emulador** (Device Manager → Create Device, imagen API 26+).
4. Pulsa **Run ▶** (o `Shift+F10`).

### Permisos en tiempo de ejecución

- **Cámara**: se solicita al abrir el escáner de QR o al sacar una foto.
- No se requieren permisos de almacenamiento: la app usa el selector del sistema
  (Storage Access Framework) y su propio almacenamiento privado.

## Notas de diseño

- **Adjuntos como archivos independientes (Opción A)**: cada foto/hoja es un
  adjunto propio. Al imprimir un registro con varias fotos, se combinan en un
  único PDF multipágina en el momento de imprimir.
- **Impresión de documentos**: los PDF y otros documentos se imprimen abriéndolos
  con la app del sistema y usando su función de impresión (combinar PDFs dentro
  de la app requeriría una librería adicional; queda como mejora futura).
- **Borrador de registro**: al crear un registro nuevo se genera un borrador
  para poder adjuntar de inmediato; si se descarta sin título ni adjuntos, se
  elimina automáticamente.

## Estado de verificación

El código está completo y estructurado para compilar con Android Studio. La
compilación no se ejecutó desde línea de comandos en el entorno de desarrollo
porque no había un JDK 17 ni Gradle disponibles fuera de Android Studio. La
verificación final (Gradle Sync + Build) debe hacerse al abrir el proyecto en
Android Studio, que provee el JDK y el Gradle Wrapper necesarios.
