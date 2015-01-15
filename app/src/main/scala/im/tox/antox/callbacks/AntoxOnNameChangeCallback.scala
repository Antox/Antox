package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
import im.tox.tox4j.core.callbacks.FriendNameCallback

import scala.None

//remove if not needed

object AntoxOnNameChangeCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnNameChangeCallback(private var ctx: Context) extends FriendNameCallback {
  override def friendName(friendNumber: Int, name: Array[Byte]): Unit = {
    val nameString = new String(name, "UTF-8")
    ToxSingleton.getAntoxFriend(friendNumber) match {
      case Some(friend) => friend.setName(nameString)
      case None => throw new Exception("Friend not found.")
    }

    val db = new AntoxDB(ctx)
    db.updateFriendName(ToxSingleton.addressFromClientId(ToxSingleton.getIdFromFriendNumber(friendNumber)), nameString)
    db.close()
    ToxSingleton.updateFriendsList(ctx)
  }
}