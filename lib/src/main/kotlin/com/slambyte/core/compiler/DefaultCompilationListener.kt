package com.slambyte.core.compiler

import java.io.File

import com.slambyte.util.filesystem.*

open class DefaultCompilationListener : ICompilationListener  {
  private var outputDir: String? = null
  private var fs: FileSystemUtil? = null

  fun setOutputDirectory(outputDir: String) {
    this.outputDir = outputDir
  }

  fun setPlatformFileSystem(fs: FileSystemUtil) {
    this.fs = fs
  }
  
  override fun onProblem(problem: CompilationProblem) {
    problem.printMessage()
  }

  override fun onClassCompiled(compiledClass: CompiledClass)  {
    if(outputDir == null) fs?.getCurrentDirectory() 
      ?: throw IllegalArgumentException("Failed getting output directory for compiled classes")
      
    val dir = fs?.createDirectory("$outputDir${File.separator}${compiledClass.fileName}") 
      ?: throw IllegalArgumentException("Failed creating directories for output class file")
      
    fs?.openOutputStream(dir)?.use  { stream ->
      stream.write(compiledClass.bytes)
      stream.flush()
    }
  }
}