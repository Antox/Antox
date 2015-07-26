package im.tox.antox.tox

import java.sql.Timestamp

import im.tox.antox.data.State
import im.tox.antox.utils.TimestampUtils
import im.tox.antox.wrapper._
import rx.lang.scala.subjects.BehaviorSubject

object Reactive {
  val chatActive = BehaviorSubject[Boolean](false)
  val chatActiveSub = chatActive.subscribe(x => State.chatActive(x))
  val activeKey = BehaviorSubject[Option[ToxKey]](None)
  val activeKeySub = activeKey.subscribe(x => State.activeKey(x))
  val typing = BehaviorSubject[Boolean](false)
}