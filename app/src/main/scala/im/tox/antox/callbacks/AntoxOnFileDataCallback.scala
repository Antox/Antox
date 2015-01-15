package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.callbacks.AntoxOnFileDataCallback._
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
//remove if not needed

object AntoxOnFileDataCallback {

  private val TAG = "OnFileDataCallback"
}

class AntoxOnFileDataCallback(private var ctx: Context) {

  def execute(friend: AntoxFriend, filenumber: Int, data: Array[Byte]) {
    Log.d(TAG, "execute")
    ToxSingleton.receiveFileData(friend.getAddress, filenumber, data, ctx)
  }
}
