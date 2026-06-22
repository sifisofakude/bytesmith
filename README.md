# Core Compiler

A lightweight Kotlin and Java compiler toolkit designed for JVM and Android environments.

The library provides a unified API for compiling Kotlin and Java sources, handling classpaths, boot classpaths, source discovery, and output packaging while remaining independent of Gradle or Maven build systems.

## Features

- Compile Kotlin source files
- Compile Java source files
- Mixed Kotlin/Java projects
- JVM support
- Android support
- Storage Access Framework (SAF) support
- Recursive source discovery
- Custom classpaths
- Custom boot classpaths
- JAR and ZIP packaging
- Pluggable filesystem abstraction
- Compilation diagnostics and reporting

---

## Installation

Maven

<dependency>
    <groupId>io.github.sifisofakude.core</groupId>
    <artifactId>compiler</artifactId>
    <version>VERSION</version>
</dependency>

Gradle Kotlin DSL

```kotlin
implementation("io.github.sifisofakude.core:compiler:VERSION")
```

---

## Quick Start

**Compile Sources**

```kotlin
val fs = JvmFileSystem()

val success = Main().compile(
    fs,
    listOf(
        "-d", "build/classes",
        "src/main/java",
        "src/main/kotlin"
    )
)
```

---

### Kotlin Compiler

Compile Kotlin source files directly.
```kotlin
val compiler = KotlinCompiler(fs)

val result = compiler.compile(
    Options(
        outputDir = "build/classes",
        kotlinSources = kotlinSources,
        javaSources = javaSources,
        classpath = emptyList(),
        bootClasspath = emptyList()
    )
)
```

---

### Java Compiler

Compile Java source files directly.
```kotlin
val compiler = JavaCompiler(fs)

val success = compiler.compile(
    Options(
        outputDir = "build/classes",
        kotlinSources = emptyList(),
        javaSources = javaSources,
        classpath = emptyList(),
        bootClasspath = emptyList()
    )
)
```

---

### Command Line Options

|Option| Description|
|-|-|
|`-cp` `-classpath`| Classpath |
|`-bc` `-bootclasspath`| Boot classpath |
|`-mp` `-module-path`| Module path |
|`-sp` `-sourcepath`| Source path |
|`-d`| Output directory or archive |

**Example:**
```text
compiler \
    -cp libs/* \
    -d build/classes \
    src/main/java \
    src/main/kotlin
```

---

### Module Layout Support

When a module path is supplied, sources are automatically resolved from:
```text
<module>/src/main/java
<module>/src/main/kotlin
```

**Example:**
```bash
compiler -mp app
```

Equivalent to:
```text
app/
└── src
    └── main
        ├── java
        └── kotlin
```
---

### Filesystem Support

The compiler uses the "FileSystemUtil" abstraction and can operate on:

**JVM Files**
```kotlin
val fs = JvmFileSystem()
```

**Android SAF**
```kotlin
val fs = AndroidSafFileSystem(context)
```

Supported SAF inputs include:

`content://...`

for:

- Sources
- Classpaths
- Boot classpaths
- Output archives

---

### Android Support

Android's compiler tooling expects traditional filesystem paths.

To support SAF resources, the compiler automatically materializes files into temporary storage before compilation.

Example:
```kotlin
fs.materialize(uri, "Sources")
```
Temporary resources are automatically removed after compilation.
```kotlin
fs.clearMaterialized("Sources")
```
**Performance Recommendation**

For large projects, dependency caches, boot classpaths, and compiler outputs, prefer application-specific external storage:

`/Android/data/<package>/files`

instead of:

`content://...`

Using application-specific storage avoids:

- SAF traversal
- URI resolution overhead
- Temporary materialization
- Additional file copying

This can significantly improve compilation performance for large projects.

SAF remains fully supported when users need access to arbitrary files selected through the Android Storage Access Framework.

---

### Compilation Diagnostics

Implement your own listener:

```kotlin
class MyListener : ICompilationListener {

    override fun hasErrors(): Boolean {
        return false
    }

    override fun onProblem(problem: CompilationProblem) {
        println(problem.message)
    }

    override fun onClassCompiled(compiledClass: CompiledClass) {
        println(compiledClass.fileName)
    }
}
```

Usage:
```kotlin
val compiler = JavaCompiler(
    fs,
    MyListener()
)
```

---

### Packaging

The compiler can output:

**Directory**

`build/classes`

**JAR**

`app.jar`

**ZIP**

`app.zip`

When a JAR or ZIP is supplied as the output destination, compiled classes are automatically packaged using JarUtil.

---

### Supported Source Types

`.kt`
`.java`

Kotlin compilation receives both Kotlin and Java sources so that Java symbols can be resolved during analysis.

Java compilation processes Java sources and can reference classes produced during Kotlin compilation.

---

## Requirements

**JVM**

- Java 8+

**Android**

- Android Storage Access Framework support
- Read/write permissions granted by the selected document provider

---

## License

MIT License
