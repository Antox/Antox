package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.av.callbacks.{CallCallback, CallStateCallback}
import im.tox.tox4j.av.enums.ToxCallState
import im.tox.tox4j.exceptions.ToxException

class AntoxOnCallCallback(private var ctx: Context) extends CallCallback {

  override def call(friendNumber: Int, audioEnabled: Boolean, videoEnabled: Boolean): Unit = {
    ToxSingleton.getAntoxFriend(friendNumber).get.call.answerCall(42, 0, audioEnabled, videoEnabled)
  }

  //override def callState(friendNumber: Int, callState: ToxCallState): Unit = {
    /*
          Log.d("OnAvCallbackCallback", "Callback type: ON_INVITE")


            case ToxAvCallbackID.ON_START =>
              Log.d("OnAvCallbackCallback", "Callback type: ON_START")
              toxSingleton.tox.avPrepareTransmission(callID, 3, 40, false)
              Methods.avStart(callID, toxCodecSettings)

            case ToxAvCallbackID.ON_CANCEL => Log.d("OnAvCallbackCallback", "Callback type: ON_CANCEL")
            case ToxAvCallbackID.ON_REJECT => Log.d("OnAvCallbackCallback", "Callback type: ON_REJECT")
            case ToxAvCallbackID.ON_END =>
              Log.d("OnAvCallbackCallback", "Callback type: ON_END")
              Methods.avEnd(callID)

            case ToxAvCallbackID.ON_RINGING => Log.d("OnAvCallbackCallback", "Callback type: ON_RINGING")
            case ToxAvCallbackID.ON_STARTING => Log.d("OnAvCallbackCallback", "Callback type: ON_STARTING")
            case ToxAvCallbackID.ON_ENDING => Log.d("OnAvCallbackCallback", "Callback type: ON_ENDING")
            case ToxAvCallbackID.ON_REQUEST_TIMEOUT => Log.d("OnAvCallbackCallback", "Callback type: ON_REQUEST_TIMEOUT")
            case ToxAvCallbackID.ON_PEER_TIMEOUT => Log.d("OnAvCallbackCallback", "Callback type: ON_PEER_TIMEOUT")
            case ToxAvCallbackID.ON_MEDIA_CHANGE => Log.d("OnAvCallbackCallback", "Callback type: ON_MEDIA_CHANGE")
          } catch {
            case e: ToxException => e.printStackTrace
          } */
  //}///
}
