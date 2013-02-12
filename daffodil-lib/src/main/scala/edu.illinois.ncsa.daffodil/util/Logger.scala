package edu.illinois.ncsa.daffodil.util

/* Copyright (c) 2012-2013 Tresys Technology, LLC. All rights reserved.
 *
 * Developed by: Tresys Technology, LLC
 *               http://www.tresys.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal with
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimers.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimers in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. Neither the names of Tresys Technology, nor the names of its contributors
 *     may be used to endorse or promote products derived from this Software
 *     without specific prior written permission.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
 * SOFTWARE.
 */

import scala.actors.Actor
import scala.actors.Actor._
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import java.io.PrintStream
import java.io.FileOutputStream

/**
 * Simple logging system evolved from code found on Stack Overflow, on the web.
 * http://stackoverflow.com/questions/2018528/logging-in-scala
 * Mostly based on the contribution of Don Mackenzie.
 */

object LogLevel extends Enumeration {
  val Error = Value(10)
  val Warning = Value(20)
  val Info = Value(30)
  val Compile = Value(35)
  val Debug = Value(40) // does not time-stamp messages. 
  val OOLAGDebug = Value(50)
}

abstract class GlobBase(lvl: LogLevel.Value) {
  // Have to do this by having an overload for each number of args.
  // This is because we're depending on scala's call-by-name trick to NOT
  // evaluate these arguments unless something else decides to force this whole adventure.
  def apply(msg: => String) = new Glob(lvl, msg, Seq())
  def apply(msg: => String, arg: => Any) = new Glob(lvl, msg, Seq(arg))
  def apply(msg: => String, arg0: => Any, arg1: => Any) = new Glob(lvl, msg, Seq(arg0, arg1))
  def apply(msg: => String, arg0: => Any, arg1: => Any, arg2: => Any) = new Glob(lvl, msg, Seq(arg0, arg1, arg2))
  def apply(msg: => String, arg0: => Any, arg1: => Any, arg2: => Any, arg3: => Any) = new Glob(lvl, msg, Seq(arg0, arg1, arg2, arg3))
  def apply(msg: => String, arg0: => Any, arg1: => Any, arg2: => Any, arg3: => Any, arg4: => Any) = new Glob(lvl, msg, Seq(arg0, arg1, arg2, arg3, arg4))
  def apply(msg: => String, arg0: => Any, arg1: => Any, arg2: => Any, arg3: => Any, arg4: => Any, arg5: => Any) = new Glob(lvl, msg, Seq(arg0, arg1, arg2, arg3, arg4, arg5))
  // add more here if more than however many args are needed.
}

object Error extends GlobBase(LogLevel.Error)

object Warning extends GlobBase(LogLevel.Warning)

object Info extends GlobBase(LogLevel.Info)

object Compile extends GlobBase(LogLevel.Compile)

object Debug extends GlobBase(LogLevel.Debug)

object OOLAGDebug extends GlobBase(LogLevel.OOLAGDebug)

trait Identity {
  def logID: String
}

abstract class LogWriter {
  protected def write(msg: String): Unit
  protected val tstampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS ")

  def tstamp = tstampFormat.format(new Date)

  def log(logID: String, glob: Glob) {
    try {
      val mess = glob.stringify
      val areStamping = glob.lvl < LogLevel.Debug
      val suffix = logID + " " + glob.lvl
      val prefix = (if (areStamping) tstamp + " " else "")
      write(prefix + suffix + " [" + mess + "]")
    } catch {
      case e: Exception => {
        System.err.println("Exception while logging." + e)
        System.err.println("Glob was: " + glob.msg + glob.args.toList.toString)
      }
    }
  }
}

object ForUnitTestLogWriter extends LogWriter {
  var loggedMsg: String = null
  //  protected val writer = actor { loop { react { case msg : String => 
  //    loggedMsg = msg
  //    Console.out.println("Was Logged: " + loggedMsg)
  //    Console.out.flush()
  //    } } }
  def write(msg: String) {
    loggedMsg = msg
  }
}

object NullLogWriter extends LogWriter {
  //protected val writer = actor { loop { react { case msg : String => } } }
  def write(msg: String) {
    // do nothing.
  }
}

object ConsoleWriter extends LogWriter {
  //  protected val writer = actor {
  //    loop {
  //      react {
  //        case msg : String =>
  //          Console.out.println(msg);
  //          Console.flush case _ =>
  //      }
  //    }
  //  }
  def write(msg: String) {
    Console.err.println(msg)
    Console.flush
  }
}

class FileWriter(val file: File) extends LogWriter {
  require(file != null)
  require(file.canWrite)

  // protected val writer = actor { loop { react { case msg : String => destFile.println(msg); destFile.flush case _ => } } }
  def write(msg: String) {
    destFile.println(msg)
    destFile.flush
  }

  private val destFile = {
    try { new PrintStream(new FileOutputStream(file)) }
    catch {
      case e =>
        ConsoleWriter.log("FileWriter", Error("Unable to create FileWriter for file %s exception was %s", file, e)); Console.out
    }
  }

}

/**
 * i18n support for logging.
 *
 * Instead of creating a string, worrying about locale-based reinterpretation, etc.
 * Just make a Glob (short for globalized message). This is intended to be passed by name,
 *
 */
class Glob(val lvl: LogLevel.Value, val msg: String, argSeq: => Seq[Any]) {
  lazy val args = argSeq
  // for now: quick and dirty English-centric approach.
  // In the future, use msg to index into i18n resource bundle for 
  // properly i18n-ized string. Can use context to avoid ambiguities.
  def stringify = {
    val res =
      try { // this can fail, if for example the string uses % for other than 
        // formats (so if the string is something that mentions DFDL entities,
        // which use % signs in their syntax.
        val str = msg.format(args: _*)
        str
      } catch {
        case e: Exception => {
          // If it fails, we punt on formatting, and just 
          // concatenate 
          val str = args.mkString(" ")
          str
        }
      }
    res
  }
}

trait Logging extends Identity {

  lazy val logID = {
    val className = getClass().getName()
    if (className endsWith "$") className.substring(0, className.length - 1)
    else className
  }

  protected var logWriter: LogWriter = LoggingDefaults.logWriter
  protected var logLevel = LoggingDefaults.logLevel

  def setLoggingLevel(level: LogLevel.Value) { logLevel = level }

  def setLogWriter(lw: LogWriter) { if (lw != null) logWriter = lw }

  def log(glob: => Glob) {
    if (logLevel >= glob.lvl) logWriter.log(logID, glob)
  }

  /**
   * Use to make debug printing over small code regions convenient. Turns on
   * your logging level of choice over a lexical region of code. Makes sure it is reset
   * to whatever it was on the exit, even if it throws.
   *
   * Call with no log level argument to turn it off (when done debugging). That way you
   * can leave it sitting there.
   */
  def withLoggingLevel[S](newLevel: LogLevel.Value = logLevel)(body: => S) = {
    val previousLogLevel = logLevel
    logLevel = newLevel
    try body
    finally {
      logLevel = previousLogLevel
    }
  }
}

object LoggingDefaults {

  var logLevel = LogLevel.Info
  var logWriter: LogWriter = ConsoleWriter

  def setLoggingLevel(level: LogLevel.Value) { logLevel = level }

  def setLogWriter(lw: LogWriter) { if (lw != null) logWriter = lw }
}