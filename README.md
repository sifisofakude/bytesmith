# ByteSmith
<p align="center">
	<img src="assets/banner.png" width="100%" alt="Bytesmith banner">
</p>
> A lightweight Kotlin and Java compiler toolkit for JVM and Android.

ByteSmith provides a unified API for compiling Kotlin, Java, and
mixed-language projects without requiring Gradle or Maven. It supports
JVM and Android, recursive source discovery, custom classpaths, Kotlin
compiler plugins, packaging, and Android Storage Access Framework (SAF).

------------------------------------------------------------------------

## Badges

``` md
![Maven Central](https://img.shields.io/maven-central/v/io.github.sifisofakude.bytesmith/bytesmith-common)
![Java](https://img.shields.io/badge/Java-8--26-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF)
![License](https://img.shields.io/badge/License-MIT-green)
```

## Features

-   Compile Kotlin
-   Compile Java
-   Mixed Kotlin/Java compilation
-   Recursive source discovery
-   JVM & Android support
-   Android SAF support
-   Kotlin compiler plugin support
-   Custom classpath & boot classpath
-   JAR & ZIP packaging
-   Warnings as errors
-   Command-line interface
-   Embeddable API
-   Pluggable filesystem abstraction

## Installation

### Maven

``` xml
<dependency>
    <groupId>io.github.sifisofakude.bytesmith</groupId>
    <artifactId>bytesmith</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Gradle

``` kotlin
implementation("io.github.sifisofakude.bytesmith:bytesmith:1.0.100")
```

## Quick Start

``` kotlin
val result = Main().compile(
    JvmFileSystem(),
    listOf(
        "-d","build/classes",
        "src/main/java",
        "src/main/kotlin"
    )
)

println(result)
```

## Java Compiler

``` kotlin
val result = JavaCompiler(JvmFileSystem()).compile(
    Options(
        outputDir = "build/classes",
        javaSources = javaSources,
        classpath = emptyList(),
        bootClasspath = emptyList(),
        warningsAsErrors = false
    )
)
```

## Kotlin Compiler

``` kotlin
val result = KotlinCompiler(JvmFileSystem()).compile(
    Options(
        outputDir = "build/classes",
        kotlinSources = kotlinSources,
        javaSources = javaSources,
        classpath = emptyList(),
        bootClasspath = emptyList(),
        warningsAsErrors = false
    )
)
```

## Command Line

``` text
bytesmith \
  -cp libs/* \
  -bc jmods \
  -d build/classes \
  src/main/java \
  src/main/kotlin
```

### Options

  Option                 Description
  ---------------------- --------------------------
  -cp, -classpath        Classpath
  -bc, -bootclasspath    Boot classpath
  -mp, -module-path      Module path
  -sp, -sourcepath       Source path
  -pl, -plugins          Kotlin plugin JARs
  -po, -plugin-options   Kotlin plugin options
  -d                     Output directory/archive
  -wErr                  Warnings as errors

## Kotlin Compiler Plugins

``` text
bytesmith \
  -pl plugins/serialization.jar \
  -po plugin:org.jetbrains.kotlin.serialization:enabled=true
```

Library API:

``` kotlin
Options(
    pluginClasspath = listOf("plugins/my-plugin.jar"),
    pluginOptions = listOf(
        "plugin:my.plugin:key=value"
    )
)
```

## Module Layout

    app/
    └── src/
        └── main/
            ├── java/
            └── kotlin/

``` text
bytesmith -mp app
```

## Filesystem Support

``` kotlin
JvmFileSystem()
```

``` kotlin
AndroidSafFileSystem(context)
```

Supports: - Sources - Classpaths - Boot classpaths - Plugin JARs -
Output archives

## Android SAF

``` kotlin
val local = fs.materialize(uri,"Sources")
```

Cleanup:

``` kotlin
fs.clearMaterialized("Sources")
```

## Diagnostics

``` kotlin
class Listener : ICompilationListener {
    override fun hasErrors() = false

    override fun onProblem(problem: CompilationProblem) {
        problem.printMessage()
    }

    override fun onClassCompiled(compiledClass: CompiledClass) {
        println(compiledClass.fileName)
    }
}
```

## CompilerResult

``` kotlin
println(result.success)
println(result.errorCount)
println(result.warningCount)
println(result.compiledClassCount)
println(result.elapseTimeMillis)
```

## Packaging

Directories:

    build/classes

Archives:

    app.jar
    app.zip

## Mixed Kotlin & Java

ByteSmith compiles Kotlin first, then compiles Java using the generated
Kotlin classes on the classpath, allowing both languages to coexist
naturally.

## Requirements

### JVM

-   Java 8--26

### Android

-   Storage Access Framework compatible

## License

MIT License
