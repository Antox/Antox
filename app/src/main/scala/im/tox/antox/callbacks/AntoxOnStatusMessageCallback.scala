package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{Hex, AntoxFriend}
import im.tox.tox4j.core.callbacks.FriendStatusMessageCallback

//remove if not needed

object AntoxOnStatusMessageCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnStatusMessageCallback(private var ctx: Context) extends FriendStatusMessageCallback {

  override def friendStatusMessage(friendNumber: Int, message: Array[Byte]): Unit = {
    val db = new AntoxDB(ctx)
    db.updateStatusMessage(ToxSingleton.tox.getClientId(friendNumber), new String(message, "UTF-8"))
    db.close()
    ToxSingleton.updateFriendsList(ctx)
  }
}
