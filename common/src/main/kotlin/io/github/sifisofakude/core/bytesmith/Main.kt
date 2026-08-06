package io.github.sifisofakude.core.bytesmith

import io.github.sifisofakude.filesystem.*
import io.github.sifisofakude.util.archive.ArchiveUtil

import org.jetbrains.kotlin.cli.common.ExitCode

import java.io.File

/**
 * Command-line entry point and orchestration layer for mixed Kotlin and Java
 * compilation.
 *
 * This class coordinates:
 *
 * - Source discovery
 * - Classpath and bootclasspath handling
 * - Kotlin compilation
 * - Java compilation
 * - Packaging compiled classes into JAR/ZIP archives
 * - Android SAF materialization when running on Android
 *
 * ## Compilation Flow
 *
 * 1. Parse command-line arguments.
 * 2. Resolve source files and compiler options.
 * 3. Materialize SAF resources when running on Android.
 * 4. Compile Kotlin sources using [KotlinCompiler].
 * 5. Compile Java sources using [JavaCompiler].
 * 6. Merge generated class files into the requested output.
 * 7. Package classes into a JAR/ZIP when required.
 * 8. Remove temporary build artifacts.
 *
 * ## Android Support
 *
 * Android's Storage Access Framework does not provide direct filesystem access.
 * To support standard compiler APIs, SAF files are temporarily copied into the
 * application's internal storage using [FileSystemUtil.materialize].
 *
 * After compilation completes, all temporary files are removed using
 * [FileSystemUtil.clearMaterialized].
 *
 * ## Supported Outputs
 *
 * - Directory containing compiled classes
 * - Existing JAR file (classes appended)
 * - New JAR file
 * - ZIP archive
 *
 * ## Supported Source Types
 *
 * - Kotlin (`.kt`)
 * - Java (`.java`)
 *
 * Kotlin compilation intentionally receives both Kotlin and Java source paths
 * so the Kotlin compiler can resolve Java symbols during analysis.
 */
class Main	{
   /**
    * Compiles Kotlin and Java source files.
    *
    * Supported command-line options:
    *
    * | Option | Description |
    * |----------|-------------|
    * | `-cp`, `-classpath` | Additional compilation classpath |
    * | `-bc`, `-bootclasspath` | Boot classpath |
    * | `-mp`, `-module-path` | Module/project path |
    * | `-sp`, `-sourcepath` | Source directory |
    * | `-d` | Output directory or archive |
    *
    * Source files may also be supplied directly:
    *
    * ```text
    * compiler Main.kt Hello.java
    * ```
    *
    * ## Android SAF Support
    *
    * When [fs] is an Android SAF implementation:
    *
    * - Source files may be SAF URIs
    * - Classpath entries may be SAF URIs
    * - Bootclasspath entries may be SAF URIs
    *
    * These resources are automatically materialized into internal storage
    * before compilation begins.
    *
    * Examples:
    *
    * ```kotlin
    * fs.materialize(contentUri, "Sources")
    * fs.materialize(classpathUri, "Classpath")
    * ```
    *
    * ## Output Handling
    *
    * If [outputDir] points to:
    *
    * - a directory → compiled classes are copied there
    * - a `.jar` → classes are packaged into a JAR
    * - a `.zip` → classes are packaged into a ZIP archive
    *
    * ## Return Value
    *
    * Returns `true` only when:
    *
    * - Kotlin compilation succeeds
    * - Java compilation succeeds
    *
    * @param fs filesystem implementation used for all file operations
    * @param args command-line arguments
    *
    * @return `true` if compilation completed successfully, otherwise `false`
    */
	fun compile(
		fs: FileSystemUtil,
		args: List<String>
	): CompilerResult	{
		var outputDir: String? = null
		var projectPath: String? = null
		var classpath = mutableListOf<String>()
		val sourceFiles = mutableListOf<String>()
		var bootClasspath = mutableListOf<String>()
		var pluginClasspath = mutableListOf<String>()
		var pluginOptions = mutableListOf<String>()

		val platformDetector = PlatformDetector()

		var warningsAsErrors = false
	
		var i = 0
		while(i < args.size)	{
			when(args[i])	{
				"-cp","-classpath" -> {
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						classpath.addAll(args[i+1].split(File.pathSeparator))
						i ++
					}
				}
				
				"-bc","-bootclasspath" -> {
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						bootClasspath.addAll(args[i+1].split(File.pathSeparator))
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
	
				"-pl","-plugins" ->	{
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						pluginClasspath.addAll(args[i+1].split(File.pathSeparator))
						i ++
					}
				}
	
				"-po","-plugin-options" ->	{
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						pluginOptions.addAll(args[i+1].split(','))
						i ++
					}
				}
	
				"-d" ->	{
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						outputDir = args[i+1]
						i ++
					}
				}
	
				"-wErr" ->	{
					warningsAsErrors = true
				}
	
				else ->	{
					if(args[i].endsWith(".java") || args[i].endsWith(".kt"))	{
						sourceFiles.add(args[i])
					}

					if(fs.isDirectory(args[i]))	{
						sourceFiles.add(args[i])
					}
				}
			}
			i ++
		}

		if(outputDir == null)	{
			outputDir = fs.getCurrentDirectory()
		}

		var kotlinOutput = outputDir
		
		var javaOutput: String = fs
			.resolvePath(".classes${System.currentTimeMillis()}")

		val s = File.separator

		var sources = if(projectPath != null)	{
			val javaSrc = "$projectPath${s}src${s}main${s}java"
			val kotlinSrc = "$projectPath${s}src${s}main${s}kotlin"
			
			listOf(
				javaSrc,
				kotlinSrc,
				*sourceFiles.toTypedArray()
			)
		}else	{
			sourceFiles
		}

		if(platformDetector.isAndroid())	{
			val name = fs.getName(kotlinOutput!!)
			kotlinOutput = if(name.endsWith(".jar"))	{
				"kotlinCompilerOutput/build/output/$name"
			}else	{
				"kotlinCompilerOutput/build/classes"
			}

			javaOutput = "javaCompilerOutput/build/classes"
			
			fs.getAndroidFilesDir()?.let	{
				File(it,kotlinOutput).apply	{
					if(!exists()) mkdirs()
					kotlinOutput = absolutePath
				}

				File(it,javaOutput).apply	{
					if(!exists()) mkdirs()
					javaOutput = absolutePath
				}
			} 

			?: throw IllegalStateException("Internal directory can not be found")

			classpath = classpath
				.map { fs.materialize(it,"Classpath")}
				.toMutableList()

			bootClasspath = bootClasspath
				.map { fs.materialize(it,"Bootclasspath") }
				.toMutableList()

			pluginClasspath = pluginClasspath
				.map { fs.materialize(it,"Plugins") }
				.toMutableList()

			sources = sources.map { fs.materialize(it,"Sources") }
		}

		val resolvedSources = fs.resolveFiles(sources,setOf("kt","java"))
		val resolvedPlugins = fs.resolveFiles(pluginClasspath,setOf("jar"))
			.map { fs.resolvePath(it.absolutePath) }

		val kotlinSources = resolvedSources.filter { it.relativePath.endsWith(".kt") }
		val javaSources = resolvedSources.filter { it.relativePath.endsWith(".java") }

		val options = Options(
			outputDir = kotlinOutput,
			kotlinSources = kotlinSources,
			javaSources = javaSources,
			classpath = classpath.toList(),
			bootClasspath = bootClasspath.toList(),
			pluginClasspath = resolvedPlugins,
			pluginOptions = pluginOptions,
			warningsAsErrors = warningsAsErrors
		)

			
		fs.createDirectory(javaOutput)

		val ktResult = KotlinCompiler(fs).compile(options)

		val javaCp = classpath.toMutableList()
		if(fs.exists(kotlinOutput!!))	{
			javaCp.add(kotlinOutput)
		}

		val jvResult = JavaCompiler(fs).compile(
			options.copy(
				outputDir = javaOutput,
				classpath = javaCp
			)
		)

		if(outputDir != null)	{
			val ext = fs.getName(outputDir)
				.substringAfterLast(".")
				
			val archiveExtensions = setOf("zip","jar")

			if(ext.isNotEmpty() && ext in archiveExtensions)	{
				val archiveUtil = ArchiveUtil(fs)

				val files = listOf(javaOutput)
				if(fs.exists(outputDir) && kotlinSources.isNotEmpty())	{
					archiveUtil.updateArchive(
						jarFile = outputDir,
						files = files
					)
				}else	{
					archiveUtil.createArchive(
						output = outputDir,
						files = files,
						autoManifest = true
					)
				}
			}else if(fs.isDirectory(outputDir))	{
				fs.listFiles(javaOutput).forEach	{ file ->
					fs.move(
						file,
						fs.resolvePath(outputDir))
				}

				if(kotlinOutput != outputDir)	{
					fs.listFiles(kotlinOutput).forEach	{ file ->
						fs.move(file,fs.resolvePath(outputDir))
					}
				}
			}
		}

		if(platformDetector.isAndroid())	{
			fs.clearMaterialized("kotlinCompilerOutput")
			fs.clearMaterialized("Classpath")
			fs.clearMaterialized("Bootclasspath")
			fs.clearMaterialized("Sources")
			fs.clearMaterialized("javaCompilerOutput")
			fs.clearMaterialized("Plugins")
		}else	{
			fs.delete(javaOutput)
		}

		return CompilerResult(
			success = jvResult.success && ktResult.success,
			errorCount = jvResult.errorCount + ktResult.errorCount,
			warningCount = jvResult.warningCount + ktResult.warningCount
		)
	}
}

fun main(args: Array<String>)	{
	if(args.isEmpty()) return
	
	val fs = JvmFileSystem()

	Main().compile(fs,args.toList())
}
