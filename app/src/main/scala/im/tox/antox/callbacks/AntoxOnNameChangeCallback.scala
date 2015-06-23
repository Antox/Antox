package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.UIUtils
import im.tox.tox4j.core.callbacks.FriendNameCallback

object AntoxOnNameChangeCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnNameChangeCallback(private var ctx: Context) extends FriendNameCallback {
  override def friendName(friendNumber: Int, nameBytes: Array[Byte]): Unit = {
    val name = UIUtils.removeNewlines(new String(nameBytes, "UTF-8"))
    ToxSingleton.getAntoxFriend(friendNumber) match {
      case Some(friend) => friend.setName(name)
      case None => throw new Exception("Friend not found.")
    }

    val db = new AntoxDB(ctx)
    db.updateContactName(ToxSingleton.getAntoxFriend(friendNumber).get.getKey, name)
    db.close()
    ToxSingleton.updateFriendsList(ctx)
  }
}