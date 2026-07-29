package io.github.sifisofakude.core.bytesmith

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

/**
 * Kotlin compiler diagnostic adapter for [ICompilationListener].
 *
 * This class bridges Kotlin compiler messages to the common compilation
 * listener API used throughout the compiler framework.
 *
 * Errors and warnings reported by the Kotlin compiler are converted into
 * [CompilationProblem] instances and forwarded to the configured
 * [ICompilationListener].
 *
 * This allows Java and Kotlin compilation pipelines to share the same
 * reporting infrastructure.
 *
 * @property listener recipient of compiler diagnostics
 */
class KotlinCompilerRequestor(
	private val listener: ICompilationListener,
) : MessageCollector {

    private var hasError = false 
    private var totalErrors = 0
    private var totalWarnings = 0
    
		var warningsAsErrors: Boolean = false

		/**
		 * Receives a compiler message from the Kotlin compiler.
		 *
		 * Error and warning messages are converted into
		 * [CompilationProblem] objects and forwarded to
		 * [ICompilationListener.onProblem].
		 *
		 * Informational and logging messages are ignored.
		 *
		 * If the reported message represents an error,
		 * [hasErrors] will subsequently return `true`.
		 *
		 * @param severity compiler message severity
		 * @param message compiler-generated message text
		 * @param location optional source location associated with the message
		 */
    override fun report(
	    severity: CompilerMessageSeverity,
	    message: String,
	    location: CompilerMessageSourceLocation?
    ) {

    	if(severity.isError || (severity.isWarning && warningsAsErrors))	{
    		hasError = true
    	}

    	if(severity.isError || severity.isWarning)	{
    		listener.onProblem(
    			CompilationProblem(
    				fileName = location?.path ?: "",
    				lineNumber = location?.line ?: -1,
    				columnNumber = location?.column ?: -1,
    				message = message,
    				severity = if(severity.isError)	{
    					totalErrors ++
    					
    					"ERROR"
    				}else	{
    					if(warningsAsErrors)	{
    						totalErrors ++
    						
    						"ERROR"
    					}else	{
    						totalWarnings ++
    						
    						"WARNING"
    					}
    				}
    			)
    		)
    	}
    }

	/**
	 * Returns whether at least one compilation error has been reported.
	 *
	 * Warning messages do not affect this state.
	 *
	 * @return `true` if an error has been encountered, otherwise `false`
	 */
   override fun hasErrors(): Boolean = hasError

   /**
    * Clears the internal error state.
    *
    * This method is invoked by the Kotlin compiler before a new compilation
    * session begins, allowing the collector to be reused safely.
    */
   override fun clear()	{
 			hasError = false
   }

   fun getTotalErrors(): Int = totalErrors
   fun getTotalWarnings(): Int = totalWarnings
}
