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
		private val fs = FileSystems.current

  @JvmOverloads
	fun compile(
		args: List<String>,
		androidMaterializer: AndroidMaterializer? = null
	): CompilerResult	{
		var outputDir: String? = null
		var mainClass: String? = null
		var projectPath: String? = null
		var classpath = mutableListOf<String>()
		val sourceFiles = mutableListOf<String>()
		var bootClasspath = mutableListOf<String>()
		var pluginClasspath = mutableListOf<String>()
		var pluginOptions = mutableListOf<String>()

		val platformDetector = PlatformDetector()

		var multiplatform = false
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
	
				"-main-class" ->	{
					if(i+1 < args.size && !args[i+1].startsWith("-"))	{
						mainClass = args[i+1]
						i ++
					}
				}
	
				"-multiplatform" ->	{
					multiplatform = true
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
			outputDir = projectPath?.let	{
					"$it${File.separator}build${File.separator}classes"
			} ?: fs.getCurrentDirectory()
			?: throw IllegalArgumentException("Nowhere to output class files")
		}

		var kotlinOutput = outputDir
		
		var javaOutput: String = fs
			.resolvePath(".classes${System.currentTimeMillis()}")

		val s = File.separator

		val sourcesInfo = SourcesInfo(
			sources = if(projectPath != null)	{
				val javaSrc = "$projectPath${s}src${s}main${s}java"
				val kotlinSrc = "$projectPath${s}src${s}main${s}kotlin"
				
				listOf(
					javaSrc,
					kotlinSrc,
					*sourceFiles.toTypedArray()
				)
			}else	{
				sourceFiles
			},
			kotlinOutput = kotlinOutput,
			javaOutput = javaOutput,
			classpath = classpath,
			pluginClasspath = pluginClasspath,
			bootClasspath = bootClasspath
		)

		if(platformDetector.isAndroid())	{
			androidMaterializer?.materialize(sourcesInfo)
		}

		val resolvedSources = fs.resolveFiles(sourcesInfo.sources,setOf("kt","java"))
		val resolvedPlugins = fs.resolveFiles(sourcesInfo.pluginClasspath,setOf("jar"))
			.map { fs.resolvePath(it.absolutePath) }

		val kotlinSources = resolvedSources.filter { it.relativePath.endsWith(".kt") }
		val javaSources = resolvedSources.filter { it.relativePath.endsWith(".java") }


		val options = Options(
			outputDir = sourcesInfo.kotlinOutput,
			kotlinSources = kotlinSources,
			javaSources = javaSources,
			classpath = sourcesInfo.classpath,
			bootClasspath = sourcesInfo.bootClasspath,
			pluginClasspath = resolvedPlugins,
			pluginOptions = pluginOptions,
			warningsAsErrors = warningsAsErrors,
			multiplatform = multiplatform
		)

			
		fs.createDirectory(sourcesInfo.javaOutput)

		val ktResult = KotlinJvmCompiler().compile(options)

		val javaCp = mutableListOf(*sourcesInfo.classpath.toTypedArray())
		if(fs.exists(sourcesInfo.kotlinOutput))	{
			javaCp.add(sourcesInfo.kotlinOutput)
		}

		val jvResult = JavaCompiler().compile(
			options.copy(
				outputDir = javaOutput,
				classpath = javaCp
			)
		)

		val outputDirFinal = FileOperation(outputDir)
		val hasExtension = !outputDirFinal.extension.isEmpty()
		val archiveExtensions = setOf("zip","jar")

		fun writeToDir()	{
			FileOperation(javaOutput)
				.listFiles()
				.forEach	{
					it.copy(outputDirFinal)
				}

			if(kotlinOutput != outputDir)	{
				FileOperation(kotlinOutput)
					.listFiles()
					.forEach	{
						it.copy(outputDir)
					}

				fs.delete(kotlinOutput)
			}
		}

		fun writeToJar()	{
			val archiveUtil = ArchiveUtil()
			
			val files = listOf(javaOutput)
			if(fs.exists(outputDir) && kotlinSources.isNotEmpty())	{
				archiveUtil.updateArchive(
					archiveFile = outputDir,
					files = files,
					mainClass = mainClass
				)
			}else	{
				archiveUtil.createArchive(
					output = outputDir,
					files = files,
					autoManifest = true,
					mainClass = mainClass
				)
			}
		}
		
		if(outputDirFinal.exists())	{
			if(outputDirFinal.isDirectory())	{
				writeToDir()
			}else	{
				val ext = outputDirFinal.extension
					
				if(ext in archiveExtensions)	{
					writeToJar()
				}
			}
		}else	{
			if(!hasExtension || outputDirFinal.extension !in archiveExtensions)	{
				writeToDir()
			}else	{
				writeToJar()
			}
		}

		if(platformDetector.isAndroid())	{
			androidMaterializer?.clearMaterialized(sourcesInfo)
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
	
	Main().compile(args.toList())
}
