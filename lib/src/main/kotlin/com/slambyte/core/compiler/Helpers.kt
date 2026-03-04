package com.slambyte.core.compiler

import java.io.File


import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants

import com.slambyte.util.filesystem.*

fun CharArray.string(): String  {
  val result = StringBuilder()
  this.forEach  {
    result.append(it)
  }
  return result.toString()
}

fun Array<CharArray>.toPackageName(): String  {
  val result = StringBuilder()
  this.forEach  {
    result.append("${it.string()}.")
  }
  return result.trim('.').toString()
}

fun Array<CharArray>.toPath(): String {
  val result = StringBuilder()
  this.forEach  {
    result.append("${it.string()}/")
  }
  return result.trim('/').toString()
}

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

data class CompilationProblem(
  val fileName: String,
  val lineNumber: Int,
  val message: String?,
  val severity: String
) {
  fun printMessage()  {
    println("[$severity]:$fileName:$lineNumber:$message")
  }
}

data class CompiledClass(
  val fileName: String,
  val bytes: ByteArray
)

class ErrorHandlingPolicy : IErrorHandlingPolicy  {
  override fun proceedOnErrors(): Boolean = true
  override fun stopOnFirstError(): Boolean = false
  override fun ignoreAllErrors(): Boolean = false
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