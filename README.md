<p align="center">
  <img src="docs/banner.png" alt="ByteSmith" width="100%">
</p>

# ByteSmith

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sifisofakude.bytesmith/bytesmith-common)](https://central.sonatype.com/artifact/io.github.sifisofakude.bytesmith/bytesmith-common)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/Java-8--26-ED8B00?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)
![Platform](https://img.shields.io/badge/Platform-JVM%20%7C%20Android-success)

A lightweight Kotlin and Java compiler toolkit for JVM and Android.

ByteSmith provides a unified API for compiling Kotlin, Java, and mixed-language projects without requiring Gradle, Maven, or any other build system. It supports compiler plugins, source discovery, custom classpaths, packaging, and Android Storage Access Framework (SAF) through a pluggable filesystem abstraction.

---

## Features

### Compilation

- Kotlin compilation
- Java compilation
- Mixed Kotlin/Java compilation
- Java 8–26 support
- Kotlin compiler plugin support
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