package chat.tox.antox.tox

import chat.tox.antox.data.State
import chat.tox.antox.wrapper._
import rx.lang.scala.subjects.BehaviorSubject

object Reactive {
  val chatActive = BehaviorSubject[Boolean](false)
  val chatActiveSub = chatActive.subscribe(x => State.chatActive(x))
  val activeKey = BehaviorSubject[Option[ToxKey]](None)
  val activeKeySub = activeKey.subscribe(x => State.activeKey(x))
  val typing = BehaviorSubject[Boolean](false)
}