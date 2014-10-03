package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.callbacks.OnFileSendRequestCallback
import AntoxOnFileSendRequestCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnFileSendRequestCallback {

  private val TAG = "OnFileSendRequestCallback"
}

class AntoxOnFileSendRequestCallback(private var ctx: Context) extends OnFileSendRequestCallback[AntoxFriend] {

  def execute(friend: AntoxFriend,
    filenumber: Int,
    filesize: Long,
    filename: Array[Byte]) {
    Log.d(TAG, "execute")
    ToxSingleton.fileSendRequest(friend.getId, filenumber, new String(filename), filesize, ctx)
  }
}
