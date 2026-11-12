package io.github.sifisofakude.core.bytesmith

import io.github.sifisofakude.filesystem.FileSystems
import io.github.sifisofakude.filesystem.FileSystemUtil
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.config.Services

/**
 * Kotlin metadata compiler wrapper.
 *
 * Compiles common Kotlin sources into Kotlin metadata suitable for
 * multiplatform projects.
 */
class KotlinMetadataCompiler(
    val listener: ICompilationListener = DefaultCompilationListener()
) {
    private val fs = FileSystems.current

    /**
     * Compiles Kotlin common sources into metadata.
     *
     * This compiler is intended for Kotlin Multiplatform common source sets.
     */
    fun compile(options: Options): CompilerResult {
        val outputPath: String? = options.outputDir
            ?.let(fs::resolvePath)
            ?: fs.getCurrentDirectory()

        val kotlinSources = options.kotlinSources
            .filter { fs.getExtension(it.relativePath) == "kt" }
            .map { it.absolutePath }

        if (kotlinSources.isEmpty()) {
            return CompilerResult(success = true)
        }

        val compilerClasspath: String? = options.classpath
            .map(fs::resolvePath)
            .joinToString(java.io.File.pathSeparator)

        val args = K2MetadataCompilerArguments().apply {
            freeArgs = kotlinSources
            destination = outputPath
            classpath = compilerClasspath

            pluginClasspaths = options.pluginClasspath
                .map(fs::resolvePath)
                .toTypedArray()

            pluginOptions = options.pluginOptions.toTypedArray()
        }

        val collector = KotlinCompilerRequestor(listener).apply {
            warningsAsErrors = options.warningsAsErrors
        }

        val exitCode = KotlinMetadataCompiler().exec(
            collector,
            Services.EMPTY,
            args
        )

        return CompilerResult(
            success = exitCode == ExitCode.OK,
            errorCount = collector.getTotalErrors(),
            warningCount = collector.getTotalWarnings()
        )
    }
}
