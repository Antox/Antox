package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.callbacks.OnVideoDataCallback
//remove if not needed
import scala.collection.JavaConversions._

class AntoxOnVideoDataCallback(private var ctx: Context) extends OnVideoDataCallback[AntoxFriend] {

  def execute(callID: Int,
    data: Array[Byte],
    width: Int,
    height: Int) {
    Log.d("OnVideoDataCallback", "Received a callback from: " + callID)
  }
}
