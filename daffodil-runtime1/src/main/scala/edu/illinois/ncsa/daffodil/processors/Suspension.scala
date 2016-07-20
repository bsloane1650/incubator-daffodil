package edu.illinois.ncsa.daffodil.processors

import edu.illinois.ncsa.daffodil.processors.unparsers.UState
import edu.illinois.ncsa.daffodil.dpath.WhereBlockedLocation
import edu.illinois.ncsa.daffodil.util.Coroutine
import edu.illinois.ncsa.daffodil.exceptions.Assert
import edu.illinois.ncsa.daffodil.util.CoroutineException
import edu.illinois.ncsa.daffodil.util.MaybeULong
import edu.illinois.ncsa.daffodil.io.DirectOrBufferedDataOutputStream

/**
 * Performs transient things like evaluates expressions, or finishes unparsing something, after
 * which it exits.
 */
abstract class TaskCoroutine(ustate: UState, mainCoroutineArg: => MainCoroutine)
    extends Coroutine[AnyRef]
    with WhereBlockedLocation {

  private lazy val mainCoroutine = mainCoroutineArg // evaluate arg once only

  protected def body(): Unit

  override final protected def run() {
    try {
      waitForResume
      body()
      Assert.invariant(isDone)
      ustate.dataOutputStream.setFinished() // closes it out which will then chain forward to next buffering DOS.
      //
      mainCoroutine.isMakingProgress = true
      resumeFinal(mainCoroutine, Suspension.NoData)
      // and since we fall through here, the task thread terminates now.
      // only tasks that aren't completed should hang around in a list
      // or on a data structure, so the coroutine should get picked up by the GC if
      // the task itself is.

    } catch {
      case ie: InterruptedException => // do nothing, mainCoroutine killed us
      case th: Throwable => {
        // tell consumer we're exiting via a throw
        // but not to rethrow it necessarily.
        val ce = new CoroutineException(th)
        resumeFinal(mainCoroutine, ce)
      }
    }
  }
}

/**
 * This object represents the "main" coroutine, the original thread of control to which
 * the corresponding task ultimately always resumes. The task coroutines do transient things like evaluate
 * expressions or finish unparsing something, and then exit, but always resume the main coroutine before doing so.
 */
class MainCoroutine(taskCoroutineArg: => TaskCoroutine)
    extends Coroutine[AnyRef] {

  override final def isMain = true

  private lazy val taskCoroutine = taskCoroutineArg // evaluate arg once only

  final def isDone = taskCoroutine.isDone

  final var isMakingProgress: Boolean = true

  /**
   * After calling this, call isDone and if that's false call isMakingProgress to
   * understand whether it is done, blocked on the exactly same situation, or blocked elsewhere.
   *
   * This status is needed to implement circular deadlock detection
   */
  override final def run() {
    resume(taskCoroutine, Suspension.NoData)
    if (!taskCoroutine.isDone) {

      Assert.invariant(taskCoroutine.isBlocked)
      if (taskCoroutine.isBlockedSameLocation) {
        //println("Blocking (No Fwd Progress) suspendable expression for " + erd.name + ", expr=" + erd.outputValueCalcExpr.get.prettyExpr)
        isMakingProgress = false
      } else if (taskCoroutine.isBlockedFirstTime) {
        //println("Blocking (First Time) suspendable expression for " + erd.name + ", expr=" + erd.outputValueCalcExpr.get.prettyExpr)
        isMakingProgress = true
      } else {
        //println("Blocking (New Location) suspendable expression for " + erd.name + ", expr=" + erd.outputValueCalcExpr.get.prettyExpr)
        isMakingProgress = true
      }
    } else {
      //println("Done: Suspension expression for " + erd.name + ", expr=" + erd.outputValueCalcExpr.get.prettyExpr)
    }
  }

}

object Suspension {
  object NoData
}

/**
 * A suspension pairs a TaskCoroutine with the main coroutine (i.e., original main thread)
 *
 * The suspension object keeps track of the state of the TaskCoroutine so you can ask
 * the object whether the TaskCoroutine isDone.
 *
 * When you run as suspension, that runs the task coroutine, which may
 * block. Running the suspension again retries the task, and you can repeat this until
 * isDone is true.
 */
abstract class Suspension(val ustate: UState) {

  protected final lazy val mainCoroutine = new MainCoroutine(taskCoroutine)

  final def isDone = mainCoroutine.isDone

  protected def taskCoroutine: TaskCoroutine

  def rd: RuntimeData

  /**
   * False if the expression blocked at the same spot, i.e.,
   * didn't make any forward progress.
   */
  final def isMakingProgress = mainCoroutine.isMakingProgress

  final def run() = mainCoroutine.run()

}

object SuspensionFactory extends SuspensionFactory

class SuspensionFactory {

  final def setup(ustate: UState, maybeKnownLengthInBits: MaybeULong): UState = {
    Assert.usage(ustate.currentInfosetNodeMaybe.isDefined)

    val original = ustate.dataOutputStream.asInstanceOf[DirectOrBufferedDataOutputStream]
    val buffered = original.addBuffered

    if (maybeKnownLengthInBits.isDefined) {
      // since we know the length of the unparsed representation that we're skipping for now,
      // that means we know the absolute position of the bits in the buffer we're creating
      // and that means alignment operations don't have to suspend waiting for this knowledge
      if (original.maybeAbsBitPos0b.isDefined) {
        // direct streams always know this, but buffered streams may not.

        val originalAbsBitPos0b = original.maybeAbsBitPos0b.get

        // we are passed this length (in bits)
        // and can use it to initialize the absolute bit pos of the buffered output stream.
        //
        // This allows us to deal with alignment regions, that is, we can determine
        // their size since we know the absolute bit position.

        buffered.setMaybeAbsBitPos0b(MaybeULong(originalAbsBitPos0b + maybeKnownLengthInBits.get))

      }
    }

    //
    // clone the ustate for use when evaluating the expression
    //
    // TODO: Performance - copying this whole state, just for OVC is painful.
    // Some sort of copy-on-write scheme would be better.
    //
    val cloneUState = ustate.cloneForSuspension(original)

    // the main-thread will carry on using the original ustate but unparsing
    // into this buffered stream.
    ustate.dataOutputStream = buffered

    cloneUState
  }
}

