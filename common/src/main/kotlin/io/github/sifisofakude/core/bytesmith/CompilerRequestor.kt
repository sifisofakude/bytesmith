package io.github.sifisofakude.core.bytesmith

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
class CompilerRequestor(private val listener: ICompilationListener) : ICompilerRequestor   {
	private var totalErrors = 0
	private var totalWarnings = 0

	var warningsAsErrors = false
	
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
      	val lineSeparatorPositions = result.lineSeparatorPositions
        val startPosition = problem.sourceStart
        
        val columnNumber = if (lineSeparatorPositions != null && startPosition >= 0) {
	        val lineNumber = problem.sourceLineNumber
	        if (lineNumber <= 1) {
            startPosition + 1
	        } else {
	          // Find the start character index of the current line
	          val previousLineEndIndex = lineSeparatorPositions[lineNumber - 2]
	          startPosition - previousLineEndIndex
	        }
        } else {
        	-1
        }
        listener.onProblem(
          CompilationProblem(
            fileName = problem.originatingFileName?.let { String(it) } ?: "",
            lineNumber = problem.sourceLineNumber,
            columnNumber = columnNumber,
            message = problem.message,
            severity = if(problem.isError())  {
            	totalErrors ++
            	
              "ERROR"
            }else {
            	if(!warningsAsErrors)	{
	            	totalWarnings ++
	            	
	              "WARNING"
              }else	{
              	totalErrors ++

              	"ERROR"
              }
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

  fun getTotalErrors(): Int = totalErrors
  fun getTotalWarnings(): Int = totalWarnings
}
