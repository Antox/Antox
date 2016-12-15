package im.tox.tox4j.impl

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

object ToxImplBase {

  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  /**
   * Calls a callback and catches any [[NonFatal]] exceptions it throws and logs them.
   *
   * @param fatal If this is false, exceptions thrown by callbacks are caught and logged.
   * @param state State to pass through the callback.
   * @param eventHandler The callback object.
   * @param callback The method to call on the callback object.
   * @tparam T The type of the callback object.
   */
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Throw"))
  def tryAndLog[ToxCoreState, T](fatal: Boolean, state: ToxCoreState, eventHandler: T)(callback: T => ToxCoreState => ToxCoreState): ToxCoreState = {
    if (!fatal) {
      try {
        callback(eventHandler)(state)
      } catch {
        case NonFatal(e) =>
          logger.warn("Exception caught while executing " + eventHandler.getClass.getName, e)
          state
      }
    } else {
      callback(eventHandler)(state)
    }
  }

}
