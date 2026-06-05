package io.github.sifisofakude.core.compiler

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector

import io.github.sifisofakude.filesystem.*

import java.io.File

class KotlinCompiler(
	val listener: ICompilationListener = DefaultCompilationListener()
)	{
	fun compile(options: Options): ExitCode {
		val fs = options.fs
		val outputPath = options.outputDir ?: fs.getCurrentDirectory()
		
		val kotlinSource = fs
			.resolveFiles(options.sourceFiles,setOf("kt"))
			.map	{ it.absolutePath }
			.toTypedArray()

		if(kotlinSource.isEmpty()) return ExitCode.OK

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
	    freeArgs = listOf(*kotlinSource)
	    destination = outputPath
	    classpath = compilerClasspath
	    noStdlib = true
	    jdkHome = null
	    noJdk = compilerNoJdk
    }
    
    val compiler = K2JVMCompiler()
    val collector = KotlinCompilerRequestor(listener)
    
    return compiler.exec(collector, org.jetbrains.kotlin.config.Services.EMPTY, args)
	}
}
