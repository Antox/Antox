package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{MessageHelper, Reactive, ToxSingleton}
import im.tox.tox4j.core.callbacks.FriendConnectionStatusCallback
import im.tox.tox4j.core.enums.ToxConnection

object AntoxOnConnectionStatusCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnConnectionStatusCallback(private var ctx: Context) extends FriendConnectionStatusCallback {

  override def friendConnectionStatus(friendNumber: Int, connectionStatus: ToxConnection): Unit = {
    val online = connectionStatus != ToxConnection.NONE

    println("connection status " + connectionStatus)
    val db = new AntoxDB(ctx)
    val friendKey = ToxSingleton.getAntoxFriend(friendNumber).get.getKey
    db.updateUserOnline(friendKey, online)
    ToxSingleton.getAntoxFriend(friendNumber).get.setOnline(online)

    if (online) {
      MessageHelper.sendUnsentMessages(ctx)
    } else {
      ToxSingleton.typingMap.put(friendKey, false)
      Reactive.typing.onNext(true)
    }
    ToxSingleton.updateFriendsList(ctx)
    ToxSingleton.updateMessages(ctx)
  }
}
