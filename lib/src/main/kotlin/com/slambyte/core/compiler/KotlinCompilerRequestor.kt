package io.github.sifisofakude.core.compiler

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

class KotlinCompilerRequestor(
	private val listener: ICompilationListener
) : MessageCollector {

    private var hasError = false 

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ) {

    	if(severity.isError)	{
    		hasError = true
    	}

    	if(severity.isError || severity.isWarning)	{
    		listener.onProblem(
    			CompilationProblem(
    				fileName = location?.path ?: "",
    				lineNumber = location?.line ?: -1,
    				message = message,
    				severity = if(severity.isError)	{
    					"ERROR"
    				}else	{
    					"WARNING"
    				}
    			)
    		)
    	}
    }

    override fun hasErrors() = hasError
    
    override fun clear()	{
    	hasError = false
    }
}
