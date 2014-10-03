package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.tox.Methods
import im.tox.antox.tox.Reactive
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.Constants
import im.tox.jtoxcore.callbacks.OnConnectionStatusCallback
import AntoxOnConnectionStatusCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnConnectionStatusCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnConnectionStatusCallback(private var ctx: Context) extends OnConnectionStatusCallback[AntoxFriend] {

  override def execute(friend: AntoxFriend, online: Boolean) {
    val db = new AntoxDB(ctx)
    db.updateUserOnline(friend.getId, online)
    val det = db.getFriendDetails(friend.getId)
    var tmp: String = null
    tmp = if (det(1) != "") det(1) else det(0)
    val epochNow = System.currentTimeMillis() / 1000
    if (epochNow - Constants.epoch > 30) {
      val tmp2 = if (online) this.ctx.getString(R.string.connection_online) else this.ctx.getString(R.string.connection_offline)
      db.addMessage(-1, friend.getId, tmp + " " + this.ctx.getString(R.string.connection_has) +
        " " +
        tmp2, true, true, true, 5)
      db.close()
    }
    if (online) {
      Methods.sendUnsentMessages(ctx)
    } else {
      ToxSingleton.typingMap.put(friend.getId, false)
      Reactive.typing.onNext(true)
    }
    ToxSingleton.updateFriendsList(ctx)
    ToxSingleton.updateMessages(ctx)
  }
}
