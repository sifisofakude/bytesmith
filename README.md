# ByteSmith

A lightweight Kotlin and Java compiler toolkit for JVM and Android.

ByteSmith provides a unified API for compiling Kotlin, Java, and mixed-language projects without requiring Gradle, Maven, or any other build system. It handles source discovery, classpaths, boot classpaths, compilation diagnostics, packaging, and Android Storage Access Framework (SAF) support through a pluggable filesystem abstraction.

---

# Features

### Compilation

- Compile Kotlin source files
- Compile Java source files
- Mixed Kotlin/Java compilation
- Recursive source discovery
- Compilation diagnostics
- Warnings as errors

### Build

- Custom classpaths
- Custom boot classpaths
- Module layout support
- JAR packaging
- ZIP packaging

### Platform Support

- JVM
- Android
- Android Storage Access Framework (SAF)

### Architecture

- Pluggable filesystem abstraction
- Independent of Gradle and Maven
- Embeddable as a library
- Command-line interface

---

# Installation

## Maven

```xml
<dependency>
    <groupId>io.github.sifisofakude.bytesmith</groupId>
    <artifactId>bytesmith</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Gradle Kotlin DSL

```kotlin
implementation("io.github.sifisofakude.bytesmith:bytesmith:1.0.0")
```

---

# Quick Start

```kotlin
val result = Main().compile(
    JvmFileSystem(),
    listOf(
        "-d", "build/classes",
        "src/main/java",
        "src/main/kotlin"
    )
)

println(result.success)
println(result.errorCount)
println(result.warningCount)
```

---

# Java Compiler

```kotlin
val compiler = JavaCompiler(JvmFileSystem())

val result = compiler.compile(
    Options(
        outputDir = "build/classes",
        javaSources = javaSources
    )
)

println(result.success)
```

---

# Kotlin Compiler

```kotlin
val compiler = KotlinCompiler(JvmFileSystem())

val result = compiler.compile(
    Options(
        outputDir = "build/classes",
        kotlinSources = kotlinSources,
        javaSources = javaSources
    )
)

println(result.success)
```

---

# Command Line Options

| Option | Description |
|---------|-------------|
| `-cp`, `-classpath` | Compilation classpath |
| `-bc`, `-bootclasspath` | Boot classpath |
| `-mp`, `-module-path` | Module path |
| `-sp`, `-sourcepath` | Source path |
| `-d` | Output directory or archive |
| `-pl`, `-plugins` | Kotlin compiler plugin classpath |
| `-po`, `-plugin-options` | Kotlin compiler plugin options |
| `-wErr` | Treat warnings as errors |

Example:

```text
bytesmith \
    -cp libs/* \
    -d build/classes \
    src/main/java \
    src/main/kotlin
```

---

# Module Layout

When a module path is supplied, ByteSmith automatically searches:

```text
<module>/
└── src
    └── main
        ├── java
        └── kotlin
```

Example:

```text
bytesmith -mp app
```

---

# Kotlin Compiler Plugins

ByteSmith forwards Kotlin compiler plugins directly to the Kotlin compiler.

Example:

```text
bytesmith \
    -pl plugins/serialization-plugin.jar \
    -po plugin:org.jetbrains.kotlin.serialization:enabled=true
```

Plugin classpaths and options are also available through `Options`:

```kotlin
Options(
    pluginClasspath = listOf("plugins/my-plugin.jar"),
    pluginOptions = listOf(
        "plugin:my.plugin:key=value"
    )
)
```

---

# Filesystem Support

ByteSmith operates through the `FileSystemUtil` abstraction.

JVM:

```kotlin
val fs = JvmFileSystem()
```

Android:

```kotlin
val fs = AndroidSafFileSystem(context)
```

Supported SAF resources include:

- Sources
- Classpaths
- Boot classpaths
- Plugin JARs
- Output archives

---

# Android Support

The Android Storage Access Framework does not expose traditional filesystem paths.

ByteSmith automatically materializes SAF resources into temporary application storage before compilation and removes them when compilation finishes.

Example:

```kotlin
fs.materialize(uri, "Sources")
```

Cleanup:

```kotlin
fs.clearMaterialized("Sources")
```

## Performance

For large projects, application-specific external storage is recommended:

```text
/Android/data/<package>/files
```

instead of:

```text
content://...
```

This avoids repeated SAF traversal and significantly improves compilation performance.

---

# Compilation Diagnostics

Implement your own listener:

```kotlin
class MyListener : ICompilationListener {

    override fun hasErrors(): Boolean = false

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
    JvmFileSystem(),
    MyListener()
)
```

---

# Packaging

Output may be one of:

Directory

```text
build/classes
```

JAR

```text
app.jar
```

ZIP

```text
app.zip
```

When a JAR or ZIP is specified, ByteSmith packages compiled classes automatically using `ArchiveUtil`.

---

# Mixed Kotlin and Java

ByteSmith compiles mixed-language projects in two stages:

1. Kotlin sources are compiled first.
2. Java sources are compiled using the generated Kotlin classes on the classpath.

This allows Java and Kotlin sources to reference one another naturally.

---

# Requirements

## JVM

- Java 8 or later

## Android

- Storage Access Framework support
- Read/write permissions granted by the selected document provider

---

# License

MIT License