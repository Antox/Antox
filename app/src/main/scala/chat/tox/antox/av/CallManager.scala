package chat.tox.antox.av

import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.CallNumber
import rx.lang.scala.subjects.BehaviorSubject

class CallManager {
  private val callsSubject = BehaviorSubject[Map[CallNumber, Call]](Map.empty[CallNumber, Call])

  def calls: Seq[Call] = callsSubject.getValue.values.toSeq
  val activeCallObservable = callsSubject.map(_.values.filter(_.active))

  def add(c: Call): Unit = {
    AntoxLog.debug("Adding call")
    callsSubject.onNext(callsSubject.getValue + (c.callNumber -> c))
    c.callStateObservable.subscribe { _ =>
      if(!c.active) {
        remove(c.callNumber)
      }
    }
  }

  def get(callNumber: CallNumber): Option[Call] = {
    callsSubject.getValue.get(callNumber)
  }

  private def remove(callNumber: CallNumber): Unit = {
    AntoxLog.debug("Removing call")
    callsSubject.onNext(callsSubject.getValue - callNumber)
  }

  def removeAndEndAll(): Unit = {
    callsSubject.getValue.foreach { case (callNumber, call) =>
      if (call.active) call.end(false)
      remove(callNumber)
    }
  }
}
