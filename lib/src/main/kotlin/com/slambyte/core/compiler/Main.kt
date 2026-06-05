package io.github.sifisofakude.core.compiler

import io.github.sifisofakude.filesystem.*
import io.github.sifisofakude.core.compiler.*

import org.jetbrains.kotlin.cli.common.ExitCode

class Main	{
	fun compile(
		fs: FileSystemUtil,
		args: List<String>
	): Boolean	{
		var outputDir: String? = null
		var projectPath: String? = null
		var classpath = mutableListOf<String>()
		val sourceFiles = mutableListOf<String>()
		var bootClasspath = mutableListOf<String>()
	
		var i = 0
		while(i < args.size)	{
			when(args[i])	{
				"-cp","-classpath" -> {
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						classpath.addAll(args[i+1].split(':'))
						i ++
					}
				}
				
				"-bc","-bootclasspath" -> {
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						bootClasspath.addAll(args[i+1].split(':'))
						i ++
					}
				}
				
				"-mp","-module-path" -> {
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						projectPath = args[i+1]
						i ++
					}
				}
	
				"-sp","-sourcepath" ->	{
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						sourceFiles.add(args[i+1])
						i ++
					}
				}
	
				"-d" ->	{
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						outputDir = args[i+1]
						i ++
					}
				}
	
				else ->	{
					if(args[i].endsWith(".java") || args[i].endsWith(".kt"))	{
						sourceFiles.add("${args[i]}")
					}
				}
			}
			i ++
		}
	
		val options = Options(
			fs = fs,
			module = projectPath,
			outputDir = outputDir,
			sourceFiles = sourceFiles,
			classpath = classpath.toList(),
			bootClasspath = bootClasspath.toList()
		)
	
		val ktExitCode = KotlinCompiler().compile(options)
		// val ktExitCode = ExitCode.OK
		val jvExitCode = JavaCompiler().compile(options)

		if(ktExitCode == ExitCode.OK && jvExitCode)	{
			return true
		}
		return false
	}
}

fun main(args: Array<String>)	{
	if(args.isEmpty()) return
	
	val fs = JvmFileSystem()
	
	Main().compile(fs,args.toList())
}
