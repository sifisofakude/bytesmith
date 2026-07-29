package io.github.sifisofakude.core.bytesmith

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector

import io.github.sifisofakude.filesystem.*

import java.io.File

/**
 * Kotlin compiler wrapper around the official Kotlin JVM compiler (K2).
 *
 * This class provides a filesystem-independent compilation API by using
 * [FileSystemUtil] for path resolution and integrating compiler diagnostics
 * with [ICompilationListener].
 *
 * Source files are compiled directly using the Kotlin compiler frontend,
 * while warnings and errors are forwarded through the configured listener.
 *
 * ## Features
 *
 * - JVM bytecode generation
 * - Custom classpath support
 * - Custom boot classpath support
 * - FileSystem abstraction support
 * - Compiler diagnostics forwarding
 *
 * ## Notes
 *
 * - Source files must be Kotlin source files (`.kt`)
 * - The Kotlin standard library is not automatically included
 * - If no boot classpath is supplied, the compiler uses the system JDK
 * - Compilation output is written to the configured destination directory
 *
 * @property fs filesystem abstraction used for path resolution
 * @property listener compilation event listener
 */
class KotlinCompiler(
	private val fs: FileSystemUtil,
	val listener: ICompilationListener = DefaultCompilationListener()
)	{
	/**
	 * Compiles Kotlin source files into JVM bytecode.
	 *
	 * The compiler configuration is derived from the supplied [Options].
	 *
	 * The following values are used:
	 *
	 * - [Options.sourceFiles] as Kotlin source inputs
	 * - [Options.outputDir] as compilation destination
	 * - [Options.classpath] as compiler classpath
	 * - [Options.bootClasspath] as JDK or runtime classes
	 *
	 * All classpath entries are resolved through
	 * [FileSystemUtil.resolvePath] before being passed to the compiler.
	 *
	 * If no source files are supplied, compilation completes immediately and
	 * returns [ExitCode.OK].
	 *
	 * Compiler diagnostics are reported through the configured
	 * [ICompilationListener].
	 *
	 * Example:
	 *
	 * ```kotlin
	 * val compiler = KotlinCompiler(fs)
	 *
	 * val result = compiler.compile(
	 *     Options(
	 *         sourceFiles = listOf("src/main/kotlin"),
	 *         classpath = emptyList(),
	 *         bootClasspath = getBootClasspath()
	 *     )
	 * )
	 * ```
	 *
	 * @param options compiler configuration
	 * @return compiler exit status returned by the Kotlin compiler
	 */
	fun compile(options: Options): ExitCode {
		val outputPath = options.outputDir ?: fs.getCurrentDirectory()
		
		val kotlinSources = listOf(
			*options.kotlinSources.toTypedArray(),
			*options.javaSources.toTypedArray()
		)
		.map { it.absolutePath }

		val sourceTest = kotlinSources.filter { it.endsWith(".kt") }

		if(sourceTest.isEmpty()) return ExitCode.OK

		val compilerClasspath = listOf(
			*options.classpath.toTypedArray(),
			*options.bootClasspath.toTypedArray()
		)
			.map {
				fs.resolvePath(it)
			}
			.joinToString(File.pathSeparator)

		val compilerNoJdk = options.bootClasspath.isNotEmpty()

    val args = K2JVMCompilerArguments().apply {
	    freeArgs = kotlinSources
	    destination = outputPath
	    classpath = compilerClasspath
	    noStdlib = true
	    jdkHome = null
	    noJdk = compilerNoJdk
	    pluginClasspaths = options.pluginClasspath.toTypedArray()
	    pluginOptions = options.pluginOptions.toTypedArray()
    }
    
    val compiler = K2JVMCompiler()
    val collector = KotlinCompilerRequestor(listener)
    
    return compiler.exec(collector, org.jetbrains.kotlin.config.Services.EMPTY, args)
	}
}
