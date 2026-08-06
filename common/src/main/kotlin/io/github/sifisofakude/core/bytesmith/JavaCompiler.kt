package io.github.sifisofakude.core.bytesmith

import java.io.File
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import java.util.Locale

//import kotlinx.coroutines.*

import org.eclipse.jdt.internal.compiler.Compiler
import org.eclipse.jdt.internal.compiler.ClassFile
import org.eclipse.jdt.internal.compiler.CompilationResult
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory

import org.eclipse.jdt.internal.compiler.env.INameEnvironment
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer

import org.eclipse.jdt.internal.compiler.batch.FileSystem

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions

import io.github.sifisofakude.filesystem.*

class EmptyBootClasspathException : Exception("No boot classpath found")

/**
 * Java compiler wrapper built on top of Eclipse JDT (ECJ).
 *
 * This compiler provides a unified compilation pipeline that works with
 * any [FileSystemUtil] implementation, including:
 *
 * - JVM filesystem (local disk)
 * - Android SAF (content:// URIs)
 * - Custom virtual filesystems
 *
 * It resolves source files, configures ECJ, compiles Java sources, and
 * forwards results through an [ICompilationListener].
 *
 * ## Features
 *
 * - Boot classpath auto-detection (JDK 8–21)
 * - Custom classpath support
 * - Modular project layout support
 * - FileSystem abstraction (JVM + Android SAF compatible)
 * - Incremental-compatible listener callbacks
 *
 * ## Output behavior
 *
 * Compiled `.class` files are emitted through:
 * [ICompilationListener.onClassCompiled]
 *
 * Compilation errors/warnings are emitted through:
 * [ICompilationListener.onProblem]
 *
 * @property fs filesystem abstraction used for reading/writing sources
 * @property listener compilation event listener (defaults to [DefaultCompilationListener])
 */
class JavaCompiler(
	private val fs: FileSystemUtil,
	private val listener: ICompilationListener = DefaultCompilationListener(),
)	{
	private val platformDetector = PlatformDetector()
	
	/**
	 * Compiles Java source files using Eclipse JDT compiler.
	 *
	 * This method performs the full compilation pipeline:
	 *
	 * 1. Resolves source files using [FileSystemUtil.resolveFiles]
	 * 2. Builds boot + user classpath
	 * 3. Configures ECJ compiler options
	 * 4. Converts sources into compilation units
	 * 5. Executes ECJ compilation
	 * 6. Sends results to [ICompilationListener]
	 *
	 * ## Options behavior
	 *
	 * - If `bootClasspath` is empty, it is auto-detected from the JVM
	 * - If `classpath` is empty, current working directory is used
	 * - If `outputDir` is null, current working directory is used
	 * - If `module` is provided, overrides source directories and output layout
	 *
	 * ## Return value
	 *
	 * Returns:
	 * - `true` if compilation finished (even with warnings)
	 * - `false` only if early failure occurs (e.g. missing sources)
	 *
	 * @param options compiler configuration (source level, classpath, etc.)
	 * @return [CompilationResult]
	 */
	fun compile(options: Options): CompilerResult	{
		val currentDir = fs.getCurrentDirectory()
		val outputDir = options.outputDir ?: currentDir
		
		val classpaths = mutableListOf<String>()
		if(options.bootClasspath.size > 0)	{
			classpaths.addAll(options.bootClasspath as Collection<String>)
		}else	{
			val bootClasspath = getBootClasspath()
			classpaths.addAll(bootClasspath)
			
			if(bootClasspath.isEmpty() && platformDetector.isAndroid())	{
				throw EmptyBootClasspathException()
			}
		}

		if(options.classpath.size > 0)	{
			classpaths.addAll(options.classpath as Collection<String>)
		}else	{
			if(currentDir != null)	{
				classpaths.add(currentDir)
			}
		}


		if(listener is DefaultCompilationListener)	{
			listener.setFileSystem(fs)
			outputDir?.let	{
				listener.setOutputDirectory(it)
			}
		}
		
		if(options.javaSources.isEmpty()) return CompilerResult(success = true)
		
		val compilerOptions = CompilerOptions()
		compilerOptions.targetJDK = options.target.toJdkVersion()
		compilerOptions.sourceLevel = options.source.toJdkVersion()
		compilerOptions.complianceLevel = options.source.toJdkVersion()

		val env = FileSystem(
			classpaths.toTypedArray(),arrayOf<String>(),StandardCharsets.UTF_8.name()
		)
		
		val policy = ErrorHandlingPolicy()
		val units = sourceUnits(options.javaSources)
		val requestor = CompilerRequestor(listener)
		val factory = DefaultProblemFactory(Locale.getDefault())

		requestor.warningsAsErrors = options.warningsAsErrors

		val compiler = Compiler(
			env,policy,compilerOptions,requestor,factory
		)

		try	{
			compiler.compile(units.toTypedArray())
		}finally	{
			env.cleanup()
		}

		return CompilerResult(
			success = requestor.getTotalErrors() == 0,
			errorCount = requestor.getTotalErrors(),
			warningCount = requestor.getTotalWarnings()
		)
	}

	/**
	 * Converts resolved [FileSource] entries into ECJ compilation units.
	 *
	 * This method reads file contents through [FileSystemUtil.openInputStream]
	 * and wraps them into [CompilationUnit] objects required by ECJ.
	 *
	 * Each unit contains:
	 * - file path
	 * - full source code text
	 * - package/type metadata (parsed inside CompilationUnit)
	 *
	 * @param files resolved Java source files
	 * @return list of compilation units ready for ECJ
	 */
	fun sourceUnits(files: List<FileSource>): List<CompilationUnit>	{
		val result = mutableListOf<CompilationUnit>()

		files.forEach	{ fileSource ->
			fs.openInputStream(fileSource.absolutePath)?.use	{ stream ->
				val sourceCode = buildString {
			    BufferedReader(
		        InputStreamReader(stream, StandardCharsets.UTF_8)
			    ).use { reader ->
		        var line = reader.readLine()
		        while (line != null) {
	            appendLine(line)
	            line = reader.readLine()
		        }
			    }
				}

				result.add(
					CompilationUnit(
						fileName = fileSource.relativePath,
						sourceCode = sourceCode
					)
				)
			}
		}
		return result
	}
}
