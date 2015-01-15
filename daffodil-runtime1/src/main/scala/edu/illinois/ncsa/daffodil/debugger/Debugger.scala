package edu.illinois.ncsa.daffodil.debugger

import edu.illinois.ncsa.daffodil.processors.PState
import edu.illinois.ncsa.daffodil.processors.Parser
import edu.illinois.ncsa.daffodil.exceptions.Assert

abstract class Debugger {
  def init(parser: Parser) {}
  def before(state: PState, parser: Parser) {}
  def after(before: PState, after: PState, parser: Parser) {}
  def beforeRepetition(state: PState, parser: Parser) {}
  def afterRepetition(before: PState, after: PState, parser: Parser) {}
  def startElement(state: PState, parser: Parser) {}
  def fini(parser: Parser) {}
}

object Debugger {

  private var debugger: Debugger = null

  /**
   * For use in test cases. Just a convenience wrapper.
   */
  def withTracing(removeHidden: Boolean = true) = {
    Debugger.setDebugging(true)
    val idbg = debugger.asInstanceOf[InteractiveDebugger] // new InteractiveDebugger(new TraceDebuggerRunner, ExpressionCompiler)
    idbg.DebuggerConfig.removeHidden = removeHidden
    Debugger.setDebugger(idbg)
  }

  /**
   * Wrap things to debug with this rather than just calling setDebugging(true).
   * That way it doesn't get turned on for every subsequent test after when
   * batches of tests are being run.
   */
  def withDebugger[T](body: => T) {
    try {
      setDebugging(true)
      body
    } finally {
      setDebugging(false)
    }
  }

  private var areDebugging = false

  def setDebugger(d: Debugger) {
    debugger = d
  }

  def setDebugging(flag: Boolean) {
    if (flag)
      Assert.usage(debugger != null, "Must call setDebugger before enabling debugging with setDebugging.")
    areDebugging = flag
  }

  def getDebugging() = {
    if (areDebugging)
      Assert.usage(debugger != null, "Debugging was enabled but no debugger was set.")
    areDebugging
  }

  def init(parser: Parser) {
    if (areDebugging) { debugger.init(parser) }
  }

  def before(state: PState, parser: Parser) {
    if (areDebugging) { debugger.before(state, parser) }
  }

  def after(before: PState, after: PState, parser: Parser) {
    if (areDebugging) { debugger.after(before, after, parser) }
  }

  def beforeRepetition(state: PState, parser: Parser) {
    if (areDebugging) { debugger.beforeRepetition(state, parser) }
  }

  def afterRepetition(before: PState, after: PState, parser: Parser) {
    if (areDebugging) { debugger.afterRepetition(before, after, parser) }
  }

  def startElement(state: PState, parser: Parser) {
    if (areDebugging) { debugger.startElement(state, parser) }
  }

  def fini(parser: Parser) {
    if (areDebugging) { debugger.fini(parser) }
  }
}
