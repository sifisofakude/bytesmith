package com.slambyte.core.compiler

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
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory

import org.eclipse.jdt.internal.compiler.env.INameEnvironment
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer

import org.eclipse.jdt.internal.compiler.batch.FileSystem

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants

import com.slambyte.util.filesystem.*

fun CharArray.string(): String	{
	val result = StringBuilder()
	this.forEach	{
		result.append(it)
	}
	return result.toString()
}

fun Array<CharArray>.toPackageName(): String	{
	val result = StringBuilder()
	this.forEach	{
		result.append("${it.string()}.")
	}
	return result.trim('.').toString()
}

fun Array<CharArray>.toPath(): String	{
	val result = StringBuilder()
	this.forEach	{
		result.append("${it.string()}/")
	}
	return result.trim('/').toString()
}

fun String.toJdkVersion(): Long	{
	var version: Long = -1
	
	when(this)	{
		"1.8" -> version = ClassFileConstants.JDK1_8
		"9" -> version = ClassFileConstants.JDK9
		"10" -> version = ClassFileConstants.JDK10
		"11" -> version = ClassFileConstants.JDK11
		"12" -> version = ClassFileConstants.JDK12
		"13" -> version = ClassFileConstants.JDK13
		"14" -> version = ClassFileConstants.JDK14
		"15" -> version = ClassFileConstants.JDK15
		"16" -> version = ClassFileConstants.JDK16
		"17" -> version = ClassFileConstants.JDK17
		"18" -> version = ClassFileConstants.JDK18
		"19" -> version = ClassFileConstants.JDK19
		"20" -> version = ClassFileConstants.JDK20
		"21" -> version = ClassFileConstants.JDK21
	}
	return version
}

fun getBootClasspath(): List<String>	{
	val javaHome = System.getProperty("java.home")

	val bootClasses = mutableListOf<String>()

	if(javaHome != null)	{
		File(javaHome).listFiles()?.forEach	{
			if(it.absolutePath == "$javaHome/jmods")	{
				it.listFiles()?.forEach	{ path ->
					bootClasses.add(path.absolutePath)
				}
			}
		}

		if(bootClasses.isEmpty())	{
			val rt = File("$javaHome/lib/rt.jar")
			if(rt.exists()) bootClasses.add(rt.absolutePath)
		}
	}
	return bootClasses.toList()
}

data class CompilationProblem(
	val fileName: String,
	val lineNumber: Int,
	val message: String?,
	val severity: String
)	{
	fun printMessage()	{
		println("[$severity]:$fileName:$lineNumber:$message")
	}
}

data class CompiledClass(
	val fileName: String,
	val bytes: ByteArray
)

class ErrorHandlingPolicy : IErrorHandlingPolicy	{
	override fun proceedOnErrors(): Boolean = true
	override fun stopOnFirstError(): Boolean = false
	override fun ignoreAllErrors(): Boolean = false
}

class CompilationUnit(sourceCode: String, fileName: String) : ICompilationUnit	{
	val contents = sourceCode
	val fileName = fileName
	
	val typeName: String
	val fullTypeName: String?
	val unitPackageName: Array<CharArray>?

	init	{
		// Define typeName variable
		val file = File(fileName)
		typeName = file.name.replace(""".java$""".toRegex(),"")

		var tmpFullTypeName: String? = null
		val tmpPackageName = mutableListOf<CharArray>()

		// Extract package name
		sourceCode.lineSequence().forEach	{ line ->
			if(line.startsWith("package"))	{
				tmpFullTypeName = line.replace(""";$""".toRegex(),"").replace("""package\s*""".toRegex(),"")
				
				val tmpPackageArr = tmpFullTypeName.split('.')
				tmpPackageArr.forEach	{
					tmpPackageName.add(it.toCharArray())
				}
			}
		}
		fullTypeName = tmpFullTypeName
		unitPackageName = tmpPackageName.toTypedArray()
	}

	override fun getFileName(): CharArray	{
		return fileName.toCharArray()
	}
	
	override fun getContents(): CharArray	{
		return contents.toCharArray()
	}

	override fun getMainTypeName(): CharArray?	{
		return typeName.toCharArray()
	}

	override fun getPackageName(): Array<CharArray>?	{
		return unitPackageName
	}
}

class CompilerRequestor(listener: CompilationListener) : ICompilerRequestor 	{
	val listener = listener
	override fun acceptResult(result: CompilationResult)	{
		var errorOccured = false
		val currentFile = result.fileName

		result.problems?.forEach	{ problem ->
			if(problem != null)	{
				listener.onProblem(
					CompilationProblem(
						fileName = problem.originatingFileName.string(),
						lineNumber = problem.sourceLineNumber,
						message = problem.message,
						severity = if(problem.isError())	{
							"ERROR"
						}else	{
							"WARNING"
						}
					)
				)
			}
		}

		if(!result.hasErrors())	{
			result.classFiles.forEach	{ classFile ->
				val fileName = "${classFile.fileName().string()}.class"
				
				listener.onClassCompiled(
					CompiledClass(
						bytes = classFile.bytes,
						fileName = "$fileName"
					)
				)
			}
		}
	}
}

data class Options(
	val fs: FileSystemUtil,
	val source: String = "17",
	val target: String = "17",
	val module: String? = null,
	val outputDir: String? = null,
	var sourceFiles: List<Any>,
	val classpath: List<String>,
	val bootClasspath: List<String>
)

interface CompilationListener	{
	fun onProblem(problem: CompilationProblem)
	fun onClassCompiled(compiledClass: CompiledClass)
}

open class DefaultCompilationListener : CompilationListener 	{
	private var outputDir: String? = null
	private var fs: FileSystemUtil? = null

	fun setOutputDirectory(outputDir: String)	{
		this.outputDir = outputDir
	}

	fun setPlatformFileSystem(fs: FileSystemUtil)	{
		this.fs = fs
	}
	
	override fun onProblem(problem: CompilationProblem)	{
		problem.printMessage()
	}

	override fun onClassCompiled(compiledClass: CompiledClass)	{
		if(outputDir == null)	fs?.getCurrentDirectory() 
			?: throw IllegalArgumentException("Failed getting output directory for compiled classes")
			
		val dir = fs?.createDirectory("$outputDir${File.separator}${compiledClass.fileName}") 
			?: throw IllegalArgumentException("Failed creating directories for output class file")
			
		fs?.openOutputStream(dir)?.use	{ stream ->
			stream.write(compiledClass.bytes)
			stream.flush()
		}
	}
}

class JavaCompiler(
	listener: CompilationListener = DefaultCompilationListener()
)	{
	val listener = listener
	val fileSep = File.separator
	
	fun compile(options: Options)	{
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
			listener.setPlatformFileSystem(fs)
			listener.setOutputDirectory(outputDir!!)
		}
		
		val resolvedFiles = fs.resolveFiles(options.sourceFiles,setOf("java"))

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
	}

	fun sourceUnits(files: List<FileSource>): List<CompilationUnit>	{
		val result = mutableListOf<CompilationUnit>()

		files.forEach	{ fileSource ->
			fileSource.stream?.use	{ stream ->
				val sourceCode = StringBuilder()
				BufferedReader(InputStreamReader(stream)).use	{
					var line: String? = it.readLine()
					while(line != null)	{
						sourceCode.append("${line}\n")
						line = it.readLine()
					}
				}

				result.add(
					CompilationUnit(
						fileName = fileSource.relativePath!!,
						sourceCode = sourceCode.toString()
					)
				)
			}
		}
		return result
	}
}

fun main(args: Array<String>)	{
	var outputDir: String? = null
	var projectPath: String? = null
	var classpath = mutableListOf<String>()
	val sourceFiles = mutableListOf<String>()
	var bootClasspath = mutableListOf<String>()

	for(i in 0..args.size-1)	{
		when(args[i])	{
			"-cp","-classpath" -> {
				if(i+1 < args.size && !args[i+1].startsWith("-"))	{
					classpath.add(args[i+1])
					continue
				}
			}
			
			"-bc","-bootclasspath" -> {
				if(i+1 < args.size && !args[i+1].startsWith("-"))	{
					bootClasspath.add(args[i+1])
					continue
				}
			}
			
			"-mp","-module-path" -> {
				if(i+1 < args.size && !args[i+1].startsWith("-"))	{
					projectPath = args[i+1]
					continue
				}
			}

			"-sp","-sourcepath" ->	{
				if(i+1 < args.size && !args[i+1].startsWith("-"))	{
					sourceFiles.add(args[i+1])
					continue
				}
			}

			"-d" ->	{
				if(i+1 < args.size && !args[i+1].startsWith("-"))	{
					outputDir = args[i+1]
				}
			}

			else ->	{
				if(args[i].endsWith(".java"))	{
					sourceFiles.add("${args[i]} ")
				}
			}
		}
	}

	JavaCompiler().compile(
		Options(
			fs = JvmFileSystem(),
			module = projectPath,
			outputDir = outputDir,
			sourceFiles = sourceFiles,
			classpath = classpath.toList(),
			bootClasspath = bootClasspath.toList()
		)
	)

	// val env = NameEnvironmentAnswer(units.toList())
	
	// println(CompilationUnit(source,"test/Hello.java").getContents().concatToString())
}
