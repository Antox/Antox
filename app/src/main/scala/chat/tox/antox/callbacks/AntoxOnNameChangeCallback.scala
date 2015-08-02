package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.UIUtils
import im.tox.tox4j.core.callbacks.FriendNameCallback

object AntoxOnNameChangeCallback {

  private val TAG = "chat.tox.antox.TAG"
}

class AntoxOnNameChangeCallback(private var ctx: Context) extends FriendNameCallback[Unit] {
  override def friendName(friendNumber: Int, nameBytes: Array[Byte])(state: Unit): Unit = {
    val name = UIUtils.removeNewlines(new String(nameBytes, "UTF-8"))
    ToxSingleton.getAntoxFriend(friendNumber) match {
      case Some(friend) => friend.setName(name)
      case None => throw new Exception("Friend not found.")
    }

    val db = State.db
    db.updateContactName(ToxSingleton.getAntoxFriend(friendNumber).get.getKey, name)
  }
}