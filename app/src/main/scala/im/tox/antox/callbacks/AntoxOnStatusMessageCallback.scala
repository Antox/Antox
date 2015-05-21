package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.UIUtils
import im.tox.tox4j.core.callbacks.FriendStatusMessageCallback

object AntoxOnStatusMessageCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnStatusMessageCallback(private var ctx: Context) extends FriendStatusMessageCallback {

  override def friendStatusMessage(friendNumber: Int, messageBytes: Array[Byte]): Unit = {
    val statusMessage = UIUtils.removeNewlines(new String(messageBytes, "UTF-8"))

    ToxSingleton.getAntoxFriend(friendNumber).get.setStatusMessage(statusMessage)

    val db = new AntoxDB(ctx)
    db.updateContactStatusMessage(ToxSingleton.tox.getFriendKey(friendNumber), statusMessage)
    db.close()
    ToxSingleton.updateFriendsList(ctx)
  }
}
