package chat.tox.antox.utils

import android.util.Log
import chat.tox.antox.utils.AntoxLog.Priority.Priority
import org.scaloid.common.LoggerTag

object AntoxLog {
  val DEFAULT_TAG = new LoggerTag("Antox")
  val CLICK_TAG = new LoggerTag("OnClick")

  object Priority extends Enumeration {
    type Priority = Value
    val ASSERT, DEBUG, ERROR, INFO, VERBOSE, WARN = Value
  }

  def log(priority: Priority, msg: String, tag: LoggerTag = DEFAULT_TAG): Unit = {
    priority match {
      case Priority.ASSERT => Log.println(Log.ASSERT, tag.tag, msg)
      case Priority.DEBUG => Log.d(tag.tag, msg)
      case Priority.ERROR => Log.e(tag.tag, msg)
      case Priority.INFO => Log.i(tag.tag, msg)
      case Priority.VERBOSE => Log.v(tag.tag, msg)
      case Priority.WARN => Log.w(tag.tag, msg)
    }
  }

  def logException(priority: Priority, msg: String, tr: Throwable, tag: LoggerTag): Unit = {
    priority match {
      case Priority.ASSERT => Log.println(Log.ASSERT, tag.tag, msg)
      case Priority.DEBUG => Log.d(tag.tag, msg, tr)
      case Priority.ERROR => Log.e(tag.tag, msg, tr)
      case Priority.INFO => Log.i(tag.tag, msg, tr)
      case Priority.VERBOSE => Log.v(tag.tag, msg, tr)
      case Priority.WARN => Log.w(tag.tag, msg, tr)
    }
  }

  def debug(msg: String, tag: LoggerTag = DEFAULT_TAG): Unit =
    log(Priority.DEBUG, msg, tag)

  def debugException(msg: String, tr: Throwable, tag: LoggerTag = DEFAULT_TAG): Unit =
    logException(Priority.DEBUG, msg, tr, tag)

  def error(msg: String, tag: LoggerTag = DEFAULT_TAG): Unit =
    log(Priority.ERROR, msg, tag)

  def errorException(msg: String, tr: Throwable, tag: LoggerTag = DEFAULT_TAG): Unit =
    logException(Priority.ERROR, msg, tr, tag)

  def info(msg: String, tag: LoggerTag = DEFAULT_TAG): Unit =
    log(Priority.INFO, msg, tag)

  def infoException(msg: String, tr: Throwable, tag: LoggerTag = DEFAULT_TAG): Unit =
    logException(Priority.INFO, msg, tr, tag)

  def verbose(msg: String, tag: LoggerTag = DEFAULT_TAG): Unit =
    log(Priority.VERBOSE, msg, tag)

  def verboseException(msg: String, tr: Throwable, tag: LoggerTag = DEFAULT_TAG): Unit =
    logException(Priority.VERBOSE, msg, tr, tag)

  def warn(msg: String, tag: LoggerTag = DEFAULT_TAG): Unit =
    log(Priority.WARN, msg, tag)

  def warnException(msg: String, tr: Throwable, tag: LoggerTag = DEFAULT_TAG): Unit =
    logException(Priority.WARN, msg, tr, tag)
}
