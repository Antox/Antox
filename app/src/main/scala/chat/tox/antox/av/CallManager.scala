package chat.tox.antox.av

import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.{CallNumber, ContactKey}
import rx.lang.scala.JavaConversions._
import rx.lang.scala.subjects.BehaviorSubject
import rx.lang.scala.{Observable, Subject}

class CallManager {
  private val callsSubject = BehaviorSubject[Map[CallNumber, Call]](Map.empty[CallNumber, Call])

  def calls: Seq[Call] = callsSubject.getValue.values.toSeq

  val activeCallObservable = callsSubject.map(_.values.filter(_.active))

  private val callAddedSubject = Subject[Call]()
  val callAddedObservable: Observable[Call] = callAddedSubject.asJavaObservable

  def add(call: Call): Unit = {
    AntoxLog.debug("Adding call")
    callAddedSubject.onNext(call)

    callsSubject.onNext(callsSubject.getValue + (call.callNumber -> call))
    call.endedObservable.subscribe { _ =>
      remove(call.callNumber)
    }
  }

  def get(callNumber: CallNumber): Option[Call] = {
    calls.find(_.callNumber == callNumber)
  }

  def get(callKey: ContactKey): Option[Call] = {
    calls.find(_.contactKey == callKey)
  }

  private def remove(callNumber: CallNumber): Unit = {
    AntoxLog.debug("Removing call")
    callsSubject.onNext(callsSubject.getValue - callNumber)
  }

  def removeAndEndAll(): Unit = {
    callsSubject.getValue.foreach { case (callNumber, call) =>
      if (call.active) call.end()
      remove(callNumber)
    }
  }
}
