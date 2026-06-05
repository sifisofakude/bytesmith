package io.github.sifisofakude.core.compiler

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

class JavaCompiler(
	listener: ICompilationListener = DefaultCompilationListener()
)	{
	val listener = listener
	val fileSep = File.separator
	
	fun compile(options: Options): Boolean	{
		val fs = options.fs
		val currentDir = fs.getCurrentDirectory()
		
		var bootclassError = false
		var outputDir: String? = null
		
		val classpaths = mutableListOf<String>()
		if(options.bootClasspath.size > 0)	{
			classpaths.addAll(options.bootClasspath as Collection<String>)
		}else	{
			val bootClasspath = getBootClasspath()
			classpaths.addAll(bootClasspath)
			
			if(bootClasspath.isEmpty())	{
				bootclassError = true
			}
		}

		if(options.classpath.size > 0)	{
			classpaths.addAll(options.classpath as Collection<String>)
		}else	{
			if(currentDir != null)	{
				classpaths.add(currentDir)
			}
		}

		if(options.outputDir != null)	{
			outputDir = options.outputDir
		}else	{
			outputDir = currentDir
		}

		if(options.module != null)	{
			outputDir = "${options.module}${fileSep}build${fileSep}classes"
			options.sourceFiles = listOf(
				"${options.module}${fileSep}src${fileSep}main${fileSep}java",
				"${options.module}${fileSep}src${fileSep}main${fileSep}kotlin"
			)
		}

		if(listener is DefaultCompilationListener)	{
			listener.setFileSystem(fs)
			listener.setOutputDirectory(outputDir!!)
		}
		
		val resolvedFiles = fs
			.resolveFiles(options.sourceFiles,setOf("java"))

		if(resolvedFiles.isEmpty()) return true
		
		val compilerOptions = CompilerOptions()
		compilerOptions.targetJDK = options.target.toJdkVersion()
		compilerOptions.sourceLevel = options.source.toJdkVersion()
		compilerOptions.complianceLevel = options.source.toJdkVersion()

		val env = FileSystem(
			classpaths.toTypedArray(),arrayOf<String>(),"UTF-8"
		) as INameEnvironment
		
		val policy = ErrorHandlingPolicy()
		val units = sourceUnits(resolvedFiles)
		val requestor = CompilerRequestor(listener)
		val factory = DefaultProblemFactory(Locale.getDefault())

		val compiler = Compiler(
			env,policy,compilerOptions,requestor,factory
		)
		compiler.compile(units.toTypedArray())

		return listener.hasErrors()
	}

	fun sourceUnits(files: List<FileSource>): List<CompilationUnit>	{
		val result = mutableListOf<CompilationUnit>()

		files.forEach	{ fileSource ->
			fileSource.stream.use	{ stream ->
				val sourceCode = StringBuilder()
				BufferedReader(InputStreamReader(stream)).use	{
					var line: String? = it.readLine()
					while(line != null)	{
						sourceCode.appendLine("${line}")
						line = it.readLine()
					}
				}

				result.add(
					CompilationUnit(
						fileName = fileSource.relativePath,
						sourceCode = sourceCode.toString()
					)
				)
			}
		}
		return result
	}
}
