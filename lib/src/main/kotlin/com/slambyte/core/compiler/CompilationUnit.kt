package io.github.sifisofakude.core.compiler

import java.io.File
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit

class CompilationUnit(sourceCode: String, fileName: String) : ICompilationUnit  {
  val contents = sourceCode
  val fileName = fileName
  
  val typeName: String
  val fullTypeName: String?
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
    fullTypeName = tmpFullTypeName
    unitPackageName = tmpPackageName.toTypedArray()
  }

  override fun getFileName(): CharArray {
    return fileName.toCharArray()
  }
  
  override fun getContents(): CharArray {
    return contents.toCharArray()
  }

  override fun getMainTypeName(): CharArray?  {
    return typeName.toCharArray()
  }

  override fun getPackageName(): Array<CharArray>?  {
    return unitPackageName
  }
}
