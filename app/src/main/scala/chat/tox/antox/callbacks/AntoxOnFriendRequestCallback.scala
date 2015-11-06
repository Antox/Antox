package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.data.State
import chat.tox.antox.tox.MessageHelper
import chat.tox.antox.utils.{AntoxNotificationManager, AntoxLog}
import chat.tox.antox.wrapper.{FriendKey, ToxKey}
import im.tox.tox4j.core.callbacks.FriendRequestCallback

class AntoxOnFriendRequestCallback(private var ctx: Context) extends FriendRequestCallback[Unit] {

  override def friendRequest(keyBytes: Array[Byte], timeDelta: Int, message: Array[Byte])(state: Unit): Unit = {
    val db = State.db
    val key = new FriendKey(keyBytes)
    if (!db.isContactBlocked(key)){
      db.addFriendRequest(key, new String(message, "UTF-8"))
    }

    AntoxLog.debug("New Friend Request")
    AntoxNotificationManager.createRequestNotification(key, Some(new String(message, "UTF-8")), ctx)
  }
}
