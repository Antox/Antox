package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.callbacks.OnStatusMessageCallback
import AntoxOnStatusMessageCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnStatusMessageCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnStatusMessageCallback(private var ctx: Context) extends OnStatusMessageCallback[AntoxFriend] {

  override def execute(friend: AntoxFriend, newStatus: String) {
    val db = new AntoxDB(ctx)
    db.updateStatusMessage(friend.getId, newStatus)
    db.close()
    ToxSingleton.updateFriendsList(ctx)
  }
}
