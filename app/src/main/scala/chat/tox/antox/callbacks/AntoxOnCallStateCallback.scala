package chat.tox.antox.callbacks

import java.util

import android.content.Context
import android.util.Log
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.CallNumber
import im.tox.tox4j.av.enums.ToxavFriendCallState

import scala.collection.JavaConversions._

class AntoxOnCallStateCallback(private var ctx: Context) {

  def callState(callNumber: CallNumber, collectionState: util.Collection[ToxavFriendCallState])(state: Unit): Unit = {
    Log.d("OnAvCallbackCallback", "Received a callback from: " + callNumber + " state is " + collectionState)

    val call = State.callManager.get(callNumber)
    val state = collectionState.toSet
    call.foreach(_.updateFriendState(state))
  }

}
