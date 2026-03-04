package com.slambyte.core.compiler

interface ICompilationListener  {
  fun onProblem(problem: CompilationProblem)
  fun onClassCompiled(compiledClass: CompiledClass)
}