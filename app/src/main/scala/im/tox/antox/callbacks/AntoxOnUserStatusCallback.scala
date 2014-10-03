package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.ToxUserStatus
import im.tox.jtoxcore.callbacks.OnUserStatusCallback
import AntoxOnUserStatusCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnUserStatusCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnUserStatusCallback(private var ctx: Context) extends OnUserStatusCallback[AntoxFriend] {

  override def execute(friend: AntoxFriend, newStatus: ToxUserStatus) {
    val db = new AntoxDB(ctx)
    db.updateUserStatus(friend.getId, newStatus)
    db.close()
    ToxSingleton.updateFriendsList(ctx)
  }
}
