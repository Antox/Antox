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
