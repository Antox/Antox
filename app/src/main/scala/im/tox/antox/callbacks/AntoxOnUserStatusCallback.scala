package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.FriendStatusCallback
import im.tox.tox4j.core.enums.ToxStatus

object AntoxOnUserStatusCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnUserStatusCallback(private var ctx: Context) extends FriendStatusCallback {

  override def friendStatus (friendNumber: Int, status: ToxStatus): Unit = {
    val db = new AntoxDB(ctx)
    db.updateContactStatus(ToxSingleton.tox.getFriendKey(friendNumber), status)
    db.close()
    ToxSingleton.
    updateFriendsList(ctx)
  }
}
