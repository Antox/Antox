package im.tox.tox4j.impl.jni

import java.io.{ PrintWriter, StringWriter }

import com.google.protobuf.InvalidProtocolBufferException
import com.typesafe.scalalogging.Logger
import im.tox.tox4j.impl.jni.proto.Value.V
import im.tox.tox4j.impl.jni.proto._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * The JNI bridge logs every call made to toxcore and toxav functions along
 * with the time taken to execute in microseconds. See the message definitions
 * in ProtoLog.proto to get an idea of what can be done with this log.
 */
// scalastyle:off non.ascii.character.disallowed
@SuppressWarnings(Array("org.wartremover.warts.Equals"))
case object ToxJniLog {

  private val logger = Logger(LoggerFactory.getLogger(getClass))

  /**
   * By default, filter out the functions called on every iteration.
   */
  filterNot(
    "tox_iterate",
    "toxav_iterate",
    "tox_iteration_interval",
    "toxav_iteration_interval"
  )

  /**
   * Set the maximum number of entries in the log. After this limit is reached,
   * logging stops and ignores any further calls until the log is fetched and cleared.
   *
   * Set to 0 to disable logging.
   */
  def maxSize_=(maxSize: Int): Unit = ToxCoreJni.tox4jSetMaxLogSize(maxSize)
  def maxSize: Int = ToxCoreJni.tox4jGetMaxLogSize

  /**
   * The current size of the log on the native side. Can be used to determine
   * whether it needs to be fetched.
   */
  def size: Int = ToxCoreJni.tox4jGetCurrentLogSize

  /**
   * Set a filter to avoid logging certain calls.
   */
  def filterNot(filter: String*): Unit = ToxCoreJni.tox4jSetLogFilter(filter.toArray)

  /**
   * Retrieve and clear the current call log. Calling [[ToxJniLog]] twice with no
   * native calls in between will return the empty log the second time. If logging
   * is disabled, this will always return the empty log.
   */
  def apply(): JniLog = {
    fromBytes(ToxCoreJni.tox4jLastLog())
  }

  /**
   * Parse a protobuf message from bytes to [[JniLog]]. Logs an error and returns
   * [[JniLog.defaultInstance]] if $bytes is invalid. Returns [[JniLog.defaultInstance]]
   * if $bytes is null.
   */
  def fromBytes(bytes: Array[Byte]): JniLog = {
    try {
      Option(bytes).map(JniLog.parseFrom).getOrElse(JniLog.defaultInstance)
    } catch {
      case e: InvalidProtocolBufferException =>
        logger.error(s"${e.getMessage}; unfinished message: ${e.getUnfinishedMessage}")
        JniLog.defaultInstance
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.While"))
  private def printDelimited[A](list: Iterable[A], separator: String)(print: A => PrintWriter => Unit)(out: PrintWriter): Unit = {
    val i = list.iterator
    if (i.hasNext) {
      print(i.next())(out)
      while (i.hasNext) { // scalastyle:ignore while
        out.print(separator)
        print(i.next())(out)
      }
    }
  }

  /**
   * Pretty-print the log as function calls with time offset from the first message. E.g.
   * [0.000000] tox_new_unique({udp_enabled=1; ipv6_enabled=0; ...}) [20 µs, #1]
   *
   * The last part is the time spent in the native function followed by the instance number.
   */
  def print(log: JniLog)(out: PrintWriter): Unit = {
    log.entries.headOption match {
      case None =>
      case Some(first) =>
        for (entry <- log.entries) {
          print(first.timestamp.getOrElse(Timestamp.defaultInstance))(entry)(out)
          out.println()
        }
    }
  }

  private def printFormattedTimeDiff(a: Timestamp, b: Timestamp)(out: PrintWriter): Unit = {
    assert(a.nanos < 1000000000)
    assert(b.nanos < 1000000000)

    val timeDiff = {
      val seconds = a.seconds - b.seconds
      val nanos = a.nanos - b.nanos
      if (nanos < 0) {
        Timestamp(seconds - 1, nanos + (1 second).toNanos.toInt)
      } else {
        Timestamp(seconds, nanos)
      }
    }

    val micros = (timeDiff.nanos nanos).toMicros.toInt
    out.print(timeDiff.seconds)
    out.print('.')
    out.print {
      // scalastyle:off if.brace
      if (false) ""
      else if (micros < 10) "00000"
      else if (micros < 100) "0000"
      else if (micros < 1000) "000"
      else if (micros < 10000) "00"
      else if (micros < 100000) "0"
      else if (micros < 1000000) ""
      // scalastyle:on if.brace
    }
    out.print(micros)
  }

  def print(startTime: Timestamp)(entry: JniLogEntry)(out: PrintWriter): Unit = {
    out.print('[')
    printFormattedTimeDiff(entry.timestamp.getOrElse(Timestamp.defaultInstance), startTime)(out)
    out.print("] ")
    out.print(entry.name)
    out.print('(')
    printDelimited(entry.arguments, ", ")(print)(out)
    out.print(") = ")
    print(entry.result.getOrElse(Value.defaultInstance))(out)
    out.print(" [")
    val elapsedNanos = entry.elapsedNanos.nanos
    if (elapsedNanos.toMicros == 0) {
      out.print(elapsedNanos.toNanos)
      out.print(" ns")
    } else {
      out.print(elapsedNanos.toMicros)
      out.print(" µs")
    }
    entry.instanceNumber match {
      case 0 =>
      case instanceNumber =>
        out.print(", #")
        out.print(instanceNumber)
    }
    out.print("]")
  }

  def print(value: Value)(out: PrintWriter): Unit = {
    value.v match {
      case V.VBytes(bytes) =>
        out.print("byte[")
        if (value.truncated == 0) {
          out.print(bytes.size)
        } else {
          out.print(value.truncated)
        }
        out.print("]")
      case V.VObject(Struct(members)) =>
        out.print('{')
        printDelimited(members, "; ")(print)(out)
        out.print('}')
      case V.VSint64(sint64) => out.print(sint64)
      case V.VString(string) => out.print(string)
      case V.Empty => out.print("void")
    }
  }

  def print(member: (String, Value))(out: PrintWriter): Unit = {
    out.print(member._1)
    out.print('=')
    print(member._2)(out)
  }

  def toString(log: JniLog): String = {
    val stringWriter = new StringWriter
    val out = new PrintWriter(stringWriter)
    print(log)(out)
    out.close()
    stringWriter.toString
  }

}
