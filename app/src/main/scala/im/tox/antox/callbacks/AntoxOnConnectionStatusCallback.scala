package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{Methods, Reactive, ToxSingleton}
import im.tox.antox.utils.{AntoxFriend, Constants}
import im.tox.tox4j.core.callbacks.{FriendConnectionStatusCallback, ConnectionStatusCallback}
import im.tox.tox4j.core.enums.ToxConnection

//remove if not needed

object AntoxOnConnectionStatusCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnConnectionStatusCallback(private var ctx: Context) extends FriendConnectionStatusCallback {

  override def friendConnectionStatus(friendNumber: Int, connectionStatus: ToxConnection): Unit = {
    val online = if(connectionStatus == ToxConnection.NONE) false else true

    val db = new AntoxDB(ctx)
    val friendAddress = ToxSingleton.addressFromClientId(ToxSingleton.getIdFromFriendNumber(friendNumber))
    db.updateUserOnline(friendAddress, online)
    val det = db.getFriendDetails(friendAddress)
    var tmp: String = null
    tmp = if (det(1) != "") det(1) else det(0)
    val epochNow = System.currentTimeMillis() / 1000
    if (epochNow - Constants.epoch > 30) {
      val tmp2 = if (online) this.ctx.getString(R.string.connection_online) else this.ctx.getString(R.string.connection_offline)
      db.addMessage(-1, friendAddress, tmp + " " + this.ctx.getString(R.string.connection_has) +
        " " +
        tmp2, has_been_received = true, has_been_read = true, successfully_sent = true, 5)
      db.close()
    }
    if (online) {
      Methods.sendUnsentMessages(ctx)
    } else {
      ToxSingleton.typingMap.put(friendAddress, false)
      Reactive.typing.onNext(true)
    }
    ToxSingleton.updateFriendsList(ctx)
    ToxSingleton.updateMessages(ctx)
  }
}
