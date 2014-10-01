package im.tox.antox.tox

import im.tox.antox.data.State
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscriber
import rx.lang.scala.Subscription
import rx.lang.scala.Subject
import rx.lang.scala.subjects.BehaviorSubject
import rx.lang.scala.schedulers.IOScheduler
import rx.lang.scala.schedulers.AndroidMainThreadScheduler

object Reactive {
  val chatActive = BehaviorSubject[Boolean](false)
  val chatActiveSub = chatActive.subscribe(x => State.chatActive(x))
  val activeKey = BehaviorSubject[Option[String]](None)
  val activeKeySub = activeKey.subscribe(x => State.activeKey(x))
}
