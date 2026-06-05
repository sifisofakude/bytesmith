package io.github.sifisofakude.core.compiler

import org.eclipse.jdt.internal.compiler.ClassFile
import org.eclipse.jdt.internal.compiler.CompilationResult
import org.eclipse.jdt.internal.compiler.ICompilerRequestor

class CompilerRequestor(listener: ICompilationListener) : ICompilerRequestor   {
  val listener = listener
  override fun acceptResult(result: CompilationResult)  {
    var errorOccured = false
    val currentFile = result.fileName

    result.problems?.forEach  { problem ->
      if(problem != null) {
        listener.onProblem(
          CompilationProblem(
            fileName = problem.originatingFileName.string(),
            lineNumber = problem.sourceLineNumber,
            message = problem.message,
            severity = if(problem.isError())  {
              "ERROR"
            }else {
              "WARNING"
            }
          )
        )
      }
    }

    if(!result.hasErrors()) {
      result.classFiles.forEach { classFile ->
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
