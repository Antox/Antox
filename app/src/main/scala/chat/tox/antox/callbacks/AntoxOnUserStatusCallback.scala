package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.FriendStatusCallback
import im.tox.tox4j.core.enums.ToxUserStatus

object AntoxOnUserStatusCallback {

  private val TAG = "chat.tox.antox.TAG"
}

class AntoxOnUserStatusCallback(private var ctx: Context) extends FriendStatusCallback[Unit] {

  override def friendStatus(friendNumber: Int, status: ToxUserStatus)(state: Unit): Unit = {
    val db = State.db
    db.updateContactStatus(ToxSingleton.tox.getFriendKey(friendNumber), status)
  }
}
