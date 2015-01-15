package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.callbacks.AntoxOnFileSendRequestCallback._
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
//remove if not needed

object AntoxOnFileSendRequestCallback {

  private val TAG = "OnFileSendRequestCallback"
}

class AntoxOnFileSendRequestCallback(private var ctx: Context) {

  def execute(friend: AntoxFriend,
    filenumber: Int,
    filesize: Long,
    filename: Array[Byte]) {
    Log.d(TAG, "execute")
    ToxSingleton.fileSendRequest(friend.getAddress, filenumber, new String(filename), filesize, ctx)
  }
}
