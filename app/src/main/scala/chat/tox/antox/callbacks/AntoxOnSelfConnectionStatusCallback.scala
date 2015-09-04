package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.SelfConnectionStatusCallback
import im.tox.tox4j.core.enums.ToxConnection
import rx.lang.scala.subjects.BehaviorSubject

import scala.collection.mutable.ArrayBuffer

object AntoxOnSelfConnectionStatusCallback {
  val connectionStatusSubject = BehaviorSubject(ToxConnection.NONE)
}

class AntoxOnSelfConnectionStatusCallback(ctx: Context) extends SelfConnectionStatusCallback[Unit] {

  override def selfConnectionStatus(toxConnection: ToxConnection)(state: Unit): Unit = {
    ToxSingleton.tox.setSelfConnectionStatus(toxConnection)

    println("got self connection status callback")
    AntoxOnSelfConnectionStatusCallback.connectionStatusSubject.onNext(toxConnection)
  }
}
