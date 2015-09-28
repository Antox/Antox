package chat.tox.antox.av

import android.util.Log

object CallManager {
  val TAG = this.getClass.getSimpleName
}

class CallManager {
  private var _calls: Map[Integer, Call] = Map[Integer, Call]()

  def add(c: Call): Unit = {
    Log.d(CallManager.TAG, "Adding call")
    _calls = _calls + (c.id -> c)
  }

  def get(id: Integer): Option[Call] = {
    _calls.get(id)
  }

  def remove(id: Integer): Unit = {
    Log.d(CallManager.TAG, "Removing call")
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
