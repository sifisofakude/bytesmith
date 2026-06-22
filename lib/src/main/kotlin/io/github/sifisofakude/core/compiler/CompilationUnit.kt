package io.github.sifisofakude.core.compiler

import java.io.File
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit

/**
 * Implementation of Eclipse Compiler for Java (ECJ)
 * [ICompilationUnit].
 *
 * A compilation unit represents a single Java source file that can be
 * compiled by ECJ.
 *
 * This implementation accepts source code as a string and extracts
 * metadata required by the compiler, including:
 *
 * - File name
 * - Main type name
 * - Package name
 *
 * Package information is automatically parsed from the source code by
 * locating the first `package` declaration.
 *
 * Example:
 *
 * ```kotlin
 * val unit = CompilationUnit(
 *     """
 *     package com.example.app;
 *
 *     public class Main {
 *     }
 *     """.trimIndent(),
 *     "Main.java"
 * )
 * ```
 *
 * @param sourceCode Java source code
 * @param fileName source file name or path
 */
class CompilationUnit(sourceCode: String, fileName: String) : ICompilationUnit  {
	/**
	 * Complete source code of the compilation unit.
	 */
	val contents = sourceCode
	
	/**
	 * Source file name or path supplied to the compiler.
	 */
	val fileName = fileName

  /**
   * Primary type name derived from the source file name.
   *
   * For example:
   *
   * ```text
   * Main.java -> Main
   * ```
   */
  val typeName: String

  /**
   * Fully qualified package name extracted from the source code.
   *
   * For example:
   *
   * ```java
   * package com.example.app;
   * ```
   *
   * produces:
   *
   * ```text
   * com.example.app
   * ```
   *
   * Returns `null` when no package declaration is present.
   */
  val packageName: String?

  /**
   * Package name represented in the format required by ECJ.
   *
   * Example:
   *
   * ```text
   * com.example.app
   * ```
   *
   * becomes:
   *
   * ```kotlin
   * arrayOf(
   *     "com".toCharArray(),
   *     "example".toCharArray(),
   *     "app".toCharArray()
   * )
   * ```
   *
   * Returns an empty array when no package declaration is found.
   */
  val unitPackageName: Array<CharArray>?

  init  {
    // Define typeName variable
    val file = File(fileName)
    typeName = file.name.replace(""".java$""".toRegex(),"")

    var tmpFullTypeName: String? = null
    val tmpPackageName = mutableListOf<CharArray>()

    // Extract package name
    for(line in sourceCode.lineSequence())	{
      if(line.startsWith("package"))  {
        tmpFullTypeName = line.replace(""";$""".toRegex(),"").replace("""package\s*""".toRegex(),"")
        
        val tmpPackageArr = tmpFullTypeName.split('.')
        tmpPackageArr.forEach {
          tmpPackageName.add(it.toCharArray())
        }
        break
      }
    }
    packageName = tmpFullTypeName
    unitPackageName = tmpPackageName.toTypedArray()
  }

	/**
	 * Returns the source file name as a character array.
	 */
  override fun getFileName(): CharArray {
    return fileName.toCharArray()
  }

  /**
   * Returns the source code contents as a character array.
   */
  override fun getContents(): CharArray {
    return contents.toCharArray()
  }

	/**
	 * Returns the primary type name being compiled.
	 *
	 * This is normally derived from the source file name.
	 */
  override fun getMainTypeName(): CharArray?  {
    return typeName.toCharArray()
  }

	/**
	 * Returns the package name in ECJ's expected format.
	 *
	 * Each package segment is represented as a separate character array.
	 */
  override fun getPackageName(): Array<CharArray>?  {
    return unitPackageName
  }
}
