package chat.tox.antox.av

import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.CallNumber
import rx.lang.scala.subjects.BehaviorSubject

class CallManager {
  private val calls = BehaviorSubject[Map[CallNumber, Call]](Map.empty[CallNumber, Call])

  val activeCallObservable = calls.map(_.values.filter(_.active))

  def add(c: Call): Unit = {
    AntoxLog.debug("Adding call")
    calls.onNext(calls.getValue + (c.callNumber -> c))
    c.callStateObservable.subscribe { _ =>
      if(!c.active) {
        remove(c.callNumber)
      }
    }
  }

  def get(callNumber: CallNumber): Option[Call] = {
    calls.getValue.get(callNumber)
  }

  private def remove(callNumber: CallNumber): Unit = {
    AntoxLog.debug("Removing call")
    calls.onNext(calls.getValue - callNumber)
  }

  def removeAndEndAll(): Unit = {
    calls.getValue.foreach { case (callNumber, call) =>
      if (call.active) call.end(false)
      remove(callNumber)
    }
  }
}
