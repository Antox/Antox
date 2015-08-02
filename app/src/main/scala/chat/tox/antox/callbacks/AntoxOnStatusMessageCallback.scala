package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.UIUtils
import im.tox.tox4j.core.callbacks.FriendStatusMessageCallback

object AntoxOnStatusMessageCallback {

  private val TAG = "chat.tox.antox.TAG"
}

class AntoxOnStatusMessageCallback(private var ctx: Context) extends FriendStatusMessageCallback[Unit] {

  override def friendStatusMessage(friendNumber: Int, messageBytes: Array[Byte])(state: Unit): Unit = {
    val statusMessage = UIUtils.removeNewlines(new String(messageBytes, "UTF-8"))

    ToxSingleton.getAntoxFriend(friendNumber).get.setStatusMessage(statusMessage)

    val db = State.db
    db.updateContactStatusMessage(ToxSingleton.tox.getFriendKey(friendNumber), statusMessage)
  }
}
