package im.tox.antox.callbacks

import android.content.{Intent, Context}
import android.util.Log
import im.tox.antox.activities.{ChatActivity, CallActivity}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.Friend
import im.tox.tox4j.av.callbacks.{CallCallback, CallStateCallback}
import rx.lang.scala.subjects.PublishSubject

class AntoxOnCallCallback(private var ctx: Context) extends CallCallback {


  override def call(friendNumber: Int, audioEnabled: Boolean, videoEnabled: Boolean): Unit = {
    val friend = ToxSingleton.getAntoxFriend(friendNumber).get
    friend.call.onIncoming(audioEnabled, videoEnabled)

    new Thread(new Runnable {
      override def run(): Unit = {
        val callActivity = new Intent(ctx, classOf[CallActivity])
        // Add avatar and nickname as extras
        callActivity.putExtra("key", friend.key.toString)
        callActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(callActivity)
      }
    }).start()
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
