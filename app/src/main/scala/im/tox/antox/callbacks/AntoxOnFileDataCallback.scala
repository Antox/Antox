package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.tox.Methods
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.callbacks.OnFileDataCallback
import AntoxOnFileDataCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnFileDataCallback {

  private val TAG = "OnFileDataCallback"
}

class AntoxOnFileDataCallback(private var ctx: Context) extends OnFileDataCallback[AntoxFriend] {

  def execute(friend: AntoxFriend, filenumber: Int, data: Array[Byte]) {
    Log.d(TAG, "execute")
    ToxSingleton.receiveFileData(friend.getId, filenumber, data, ctx)
  }
}
