package im.tox.antox.utils

import android.util.Log
import im.tox.antox.utils.CallList._

object CallList {
  private val TAG = "im.tox.antox.utils.CallManager"
}

class CallList () {
  private var _calls: Map[Integer, Call] = Map[Integer, Call]()

  def add(c: Call) = {
    Log.d(TAG, "Adding call")
    _calls = _calls + (c.friendNumber -> c)
  }

  def get(friendNumber: Integer): Option[Call] = {
    _calls.get(friendNumber)
  }

  def remove(friendNumber: Integer): Unit = {
    Log.d(TAG, "Removing call")
    val mCall = this.get(friendNumber)
    mCall match {
      case Some(c) => 
        c.end()
        c.playAudio.cleanUp()
        _calls = _calls - friendNumber
      case None =>
    }
  }

  def removeAll(): Unit = {
    _calls.foreach { case (k, call) => call.end() }
  }
}
