package im.tox.antox.callbacks

import android.content.Context

class AntoxOnAvCallbackCallback(private var ctx: Context){

  //val toxCodecSettings = new ToxCodecSettings(ToxCallType.TYPE_AUDIO, 500,
  //  1280, 720, 64000, 20, 48000, 1)

  //def execute(callID: Int, callbackID: ToxAvCallbackID) {
   /* Log.d("OnAvCallbackCallback", "Received a callback from: " + callID)
    val toxSingleton = ToxSingleton.getInstance
    try callbackID match {
      case ToxAvCallbackID.ON_INVITE =>
        Log.d("OnAvCallbackCallback", "Callback type: ON_INVITE")
        Methods.avAnswer(callID, toxCodecSettings)

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
      case e: ToxException[_] => e.printStackTrace
    } */
  //}
}
