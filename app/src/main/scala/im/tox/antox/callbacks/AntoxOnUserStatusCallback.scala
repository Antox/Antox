package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{Hex, AntoxFriend}
import im.tox.tox4j.core.callbacks.FriendStatusCallback
import im.tox.tox4j.core.enums.ToxStatus

//remove if not needed

object AntoxOnUserStatusCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnUserStatusCallback(private var ctx: Context) extends FriendStatusCallback {

  override def friendStatus (friendNumber: Int, status: ToxStatus): Unit = {
    println("USER STATUS " + friendNumber + " IS NOW " + status)
    val db = new AntoxDB(ctx)
    db.updateUserStatus(ToxSingleton.tox.getClientId(friendNumber), status)
    db.close()
    ToxSingleton.
    updateFriendsList(ctx)
  }
}
