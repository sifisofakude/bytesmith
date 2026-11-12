package io.github.sifisofakude.core.bytesmith

/**
 * Listener interface for receiving compilation events.
 *
 * This interface is used by the compiler pipeline to report:
 * - diagnostic messages (errors and warnings)
 * - successfully generated class files
 *
 * Implementations can use this to:
 * - log compilation output
 * - collect errors for build failure handling
 * - write compiled bytecode to disk or memory
 * - integrate with IDE or tooling feedback systems
 */
interface ICompilationListener {

  /**
   * Returns whether any compilation errors have been encountered.
   *
   * This is typically updated internally when [onProblem] receives
   * a problem with severity `ERROR`.
   *
   * @return `true` if at least one error occurred, otherwise `false`
   */
  fun hasErrors(): Boolean

  /**
   * Called when the compiler reports a problem (error or warning).
   *
   * This may be invoked multiple times during a single compilation
   * session. Each call represents one diagnostic message.
   *
   * @param problem the compilation issue reported by the compiler
   */
  fun onProblem(problem: CompilationProblem)

  /**
   * Called when a class has been successfully compiled.
   *
   * This method is only invoked if the compilation unit produces
   * valid bytecode.
   *
   * Implementations typically write the byte array to disk,
   * memory, or a virtual filesystem.
   *
   * @param compiledClass the generated `.class` file and metadata
   */
  fun onClassCompiled(compiledClass: CompiledClass)
}
