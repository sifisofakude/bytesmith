package io.github.sifisofakude.core.compiler

import java.io.File


import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants

import io.github.sifisofakude.filesystem.*

/**
 * Converts a [CharArray] into a [String].
 *
 * This utility is primarily used when interacting with the Eclipse Compiler
 * for Java (ECJ), which frequently exposes identifiers, filenames, and
 * package components as character arrays.
 *
 * @return the contents of this character array as a string
 */
fun CharArray.string(): String  {
  val result = StringBuilder()
  this.forEach  {
    result.append(it)
  }
  return result.toString()
}

/**
 * Converts a package name represented as an array of character arrays into
 * its dot-separated form.
 *
 * Example:
 *
 * ```kotlin
 * arrayOf(
 *   "com".toCharArray(),
 *   "example".toCharArray(),
 *   "app".toCharArray()
 * ).toPackageName()
 *
 * // com.example.app
 * ```
 *
 * @return the package name using dot notation
 */
fun Array<CharArray>.toPackageName(): String  {
  val result = StringBuilder()
  this.forEach  {
    result.append("${it.string()}.")
  }
  return result.trim('.').toString()
}

/**
 * Converts a package name represented as an array of character arrays into
 * a filesystem path.
 *
 * Example:
 *
 * ```kotlin
 * arrayOf(
 *   "com".toCharArray(),
 *   "example".toCharArray()
 * ).toPath()
 *
 * // com/example
 * ```
 *
 * This is useful when generating output directories for compiled classes.
 *
 * @return the package path using forward slashes
 */
fun Array<CharArray>.toPath(): String {
  val result = StringBuilder()
  this.forEach  {
    result.append("${it.string()}/")
  }
  return result.trim('/').toString()
}

/**
 * Converts a Java language version string into the corresponding ECJ
 * class file constant.
 *
 * Supported versions:
 *
 * - 1.8
 * - 9 through 21
 *
 * Example:
 *
 * ```kotlin
 * val version = "17".toJdkVersion()
 * ```
 *
 * @return the matching ECJ JDK constant, or `-1` if unsupported
 */
fun String.toJdkVersion(): Long {
  var version: Long = -1
  
  when(this)  {
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

/**
 * Discovers the JVM boot classpath.
 *
 * On modern JDKs (9+), this method returns all modules located in the
 * `jmods` directory.
 *
 * On older JDKs (8 and below), it falls back to `rt.jar`.
 *
 * The returned paths can be supplied to a compiler so that standard Java
 * runtime classes are available during compilation.
 *
 * @return a list of boot classpath entries
 */
fun getBootClasspath(): List<String>  {
  val javaHome = System.getProperty("java.home")

  val bootClasses = mutableListOf<String>()

  if(javaHome != null)  {
    File(javaHome).listFiles()?.forEach {
      if(it.absolutePath == "$javaHome/jmods")  {
        it.listFiles()?.forEach { path ->
          bootClasses.add(path.absolutePath)
        }
      }
    }

    if(bootClasses.isEmpty()) {
      val rt = File("$javaHome/lib/rt.jar")
      if(rt.exists()) bootClasses.add(rt.absolutePath)
    }
  }
  return bootClasses.toList()
}

/**
 * Represents a compiler warning or error produced during compilation.
 *
 * Instances of this class are delivered through
 * [ICompilationListener.onProblem].
 *
 * @property fileName source file associated with the problem
 * @property lineNumber line number where the problem occurred
 * @property message compiler-generated description
 * @property severity severity level such as `ERROR` or `WARNING`
 */
data class CompilationProblem(
  val fileName: String,
  val lineNumber: Int,
  val message: String?,
  val severity: String
) {
	/**
	 * Prints the problem in a compiler-style format.
	 *
	 * Example:
	 *
	 * ```text
	 * [ERROR]:Main.java:12:cannot find symbol
	 * ```
	 */
  fun printMessage()  {
  	val fileIndicator = if(fileName.isEmpty())	{
  		""
  	}else	{
  		"$fileName:$lineNumber:"
  	}
    println("[$severity]:$fileIndicator$message")
  }
}

/**
 * Represents a generated Java class file.
 *
 * Instances are delivered through
 * [ICompilationListener.onClassCompiled].
 *
 * @property fileName relative output file name
 * @property bytes compiled class file contents
 */
data class CompiledClass(
  val fileName: String,
  val bytes: ByteArray
)

/**
 * Default ECJ error handling policy.
 *
 * This policy allows compilation to continue after errors are encountered,
 * enabling the compiler to report multiple problems in a single pass.
 *
 * This behavior is useful for IDEs, build systems, and batch compilation
 * workflows where collecting all diagnostics is preferred over failing
 * immediately.
 */
class ErrorHandlingPolicy : IErrorHandlingPolicy  {
  override fun proceedOnErrors(): Boolean = true
  override fun stopOnFirstError(): Boolean = false
  override fun ignoreAllErrors(): Boolean = false
}

/**
 * Compilation configuration shared by the Kotlin and Java compilers.
 *
 * Source files are resolved before compilation and separated into
 * language-specific collections.
 *
 * @property outputDir destination directory for generated class files
 * @property kotlinSources resolved Kotlin source files
 * @property javaSources resolved Java source files
 * @property classpath compilation classpath entries
 * @property bootClasspath runtime and platform classes used during compilation
 */
data class Options(
  val source: String = "17",
  val target: String = "17",
  val module: String? = null,
  val outputDir: String? = null,
  val kotlinSources: List<FileSource> = emptyList(),
  val javaSources: List<FileSource> = emptyList(),
  val classpath: List<String>,
  val bootClasspath: List<String>
)
