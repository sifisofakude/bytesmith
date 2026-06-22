package io.github.sifisofakude.core.compiler

import org.eclipse.jdt.internal.compiler.ClassFile
import org.eclipse.jdt.internal.compiler.CompilationResult
import org.eclipse.jdt.internal.compiler.ICompilerRequestor

/**
 * ECJ compilation result handler.
 *
 * Receives compilation results produced by the Eclipse Compiler for
 * Java (ECJ) and forwards them to an [ICompilationListener].
 *
 * This requestor is responsible for:
 *
 * - Reporting compilation errors and warnings
 * - Extracting generated class files
 * - Delivering compiled bytecode to listeners
 *
 * For each compiler problem encountered, a [CompilationProblem]
 * instance is created and passed to
 * [ICompilationListener.onProblem].
 *
 * If compilation succeeds without errors, every generated
 * [CompiledClass] is delivered through
 * [ICompilationListener.onClassCompiled].
 *
 * @param listener listener that receives compiler events
 */
class CompilerRequestor(listener: ICompilationListener) : ICompilerRequestor   {
  /**
   * Listener that receives compilation events.
   */
  val listener = listener

  /**
   * Processes a completed ECJ compilation result.
   *
   * All reported problems are converted into
   * [CompilationProblem] instances and forwarded to the
   * configured listener.
   *
   * If compilation completed without errors, generated class
   * files are extracted from the result and delivered as
   * [CompiledClass] instances.
   *
   * Warnings do not prevent class files from being emitted.
   *
   * @param result compilation result produced by ECJ
   */
  override fun acceptResult(result: CompilationResult)  {
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
