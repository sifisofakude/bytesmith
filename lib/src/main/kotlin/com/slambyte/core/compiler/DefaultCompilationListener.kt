package io.github.sifisofakude.core.compiler

import java.io.File

import io.github.sifisofakude.filesystem.*

open class DefaultCompilationListener : ICompilationListener  {
  private var outputDir: String? = null
  private var fs: FileSystemUtil? = null
  private var hasError = false

  fun setOutputDirectory(outputDir: String) {
    this.outputDir = outputDir
  }

  fun setFileSystem(fs: FileSystemUtil) {
    this.fs = fs
  }

  override fun hasErrors() = hasError
  
  override fun onProblem(problem: CompilationProblem) {
  	if(problem.severity == "ERROR")	{
  		hasError = true
  	}
    problem.printMessage()
  }

  override fun onClassCompiled(compiledClass: CompiledClass)  {
    if(outputDir == null) outputDir = fs?.getCurrentDirectory() 
      ?: throw IllegalStateException("Failed getting output directory for compiled classes")
      
    val dir = fs?.createDirectory("$outputDir${File.separator}${compiledClass.fileName}") 
      ?: throw IllegalStateException("Failed creating directories for output class file")
      
    fs?.openOutputStream(dir)?.use  { stream ->
      stream.write(compiledClass.bytes)
      stream.flush()
    }
  }
}
