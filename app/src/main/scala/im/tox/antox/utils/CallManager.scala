package im.tox.antox.utils

import android.util.Log
import im.tox.antox.utils.CallManager._

object CallManager {
  private val TAG = "im.tox.antox.utils.CallManager"
}

class CallManager () {
  private var _calls: Map[Integer, Call] = Map[Integer, Call]()

  def add(c: Call) = {
    Log.d(TAG, "Adding call")
    _calls = _calls + (c.id -> c)
  }

  def get(id: Integer): Option[Call] = {
    _calls.get(id)
  }

  def remove(id: Integer): Unit = {
    Log.d(TAG, "Removing call")
    val mCall = this.get(id)
    mCall match {
      case Some(c) => 
        c.subscription.unsubscribe()
        c.playAudio.cleanUp()
        _calls = _calls - id
      case None =>
    }
  }

  def removeAll(): Unit = {
    _calls.foreach { case (k, call) => call.subscription.unsubscribe() }
  }
}
