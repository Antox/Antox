package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.utils.{AntoxLog, AntoxNotificationManager}
import chat.tox.antox.wrapper.FriendKey
import im.tox.tox4j.core.callbacks.FriendRequestCallback
import im.tox.tox4j.core.data.{ToxFriendRequestMessage, ToxPublicKey}

class AntoxOnFriendRequestCallback(private var ctx: Context) extends FriendRequestCallback[Unit] {

  override def friendRequest(publicKey: ToxPublicKey, timeDelta: Int, message: ToxFriendRequestMessage)(state: Unit): Unit = {
    val db = State.db
    val key = new FriendKey(publicKey.toHexString)
    if (!db.isContactBlocked(key)){
      db.addFriendRequest(key, new String(message.value))
    }

    AntoxLog.debug("New Friend Request")
    AntoxNotificationManager.createRequestNotification(key, Some(new String(message.value)), ctx)
  }
}
