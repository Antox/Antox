package chat.tox.antox.callbacks

import android.content.{Intent, Context}
import chat.tox.antox.activities.CallActivity
import chat.tox.antox.av.Call
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.CallNumber

class AntoxOnCallCallback(private var ctx: Context) {

  def call(callNumber: CallNumber, audioEnabled: Boolean, videoEnabled: Boolean)(state: Unit): Unit = {
    AntoxLog.debug("New call from " + callNumber)

    try {
      State.callManager.add(new Call(callNumber, ToxSingleton.tox.getFriendKey(callNumber.value), incoming = true))
      State.callManager.get(callNumber).foreach { call =>
        call.onIncoming(audioEnabled, videoEnabled)

        new Thread(new Runnable {
          override def run(): Unit = {
            val callActivity = new Intent(ctx, classOf[CallActivity])

            callActivity.putExtra("key", call.contactKey.toString)
            callActivity.putExtra("call_number", call.callNumber.value)
            callActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(callActivity)
          }
        }).start()
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}
