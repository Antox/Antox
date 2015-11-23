package chat.tox.antox.av

import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.CallNumber

import scala.collection.mutable

class CallManager {
  private var calls: mutable.Map[CallNumber, Call] = mutable.Map[CallNumber, Call]()

  def add(c: Call): Unit = {
    AntoxLog.debug("Adding call")
    calls += (c.callNumber -> c)
  }

  def get(callNumber: CallNumber): Option[Call] = {
    calls.get(callNumber)
  }

  def remove(callNumber: CallNumber): Unit = {
    AntoxLog.debug("Removing call")
    calls.remove(callNumber)
  }

  def removeAndEndAll(): Unit = {
    calls.foreach { case (callNumber, call) =>
      if (call.active) call.end(false)
      remove(callNumber)
    }
  }
}
