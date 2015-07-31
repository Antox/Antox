package im.tox.antox.callbacks

import java.util

import android.content.Context
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.SelfConnectionStatusCallback
import im.tox.tox4j.core.enums.ToxConnection

import scala.collection.mutable.ArrayBuffer

trait SelfConnectionStatusChangeListener {
  def onSelfConnectionStatusChange(toxConnection: ToxConnection)
}

object AntoxOnSelfConnectionStatusCallback {
  private val listenerList = new ArrayBuffer[SelfConnectionStatusChangeListener]()

  def addConnectionStatusChangeListener(listener: SelfConnectionStatusChangeListener) = {
    listenerList += listener
  }

}
class AntoxOnSelfConnectionStatusCallback(ctx: Context) extends SelfConnectionStatusCallback[Unit] {

  override def selfConnectionStatus(toxConnection: ToxConnection)(state: Unit): Unit = {
    ToxSingleton.tox.setSelfConnectionStatus(toxConnection)

    println("got self connection status callback")
    for (listener <- AntoxOnSelfConnectionStatusCallback.listenerList) {
      println("sending change to listener")
      listener.onSelfConnectionStatusChange(toxConnection)
    }
  }
}
