package io.github.sifisofakude.core.compiler

interface ICompilationListener  {
	fun hasErrors(): Boolean
  fun onProblem(problem: CompilationProblem)
  fun onClassCompiled(compiledClass: CompiledClass)
}
