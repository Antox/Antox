package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.wrapper.MessageType
import im.tox.tox4j.core.callbacks.FriendMessageCallback
import im.tox.tox4j.core.enums.ToxMessageType

object AntoxOnMessageCallback {

  val TAG = "chat.tox.antox.callbacks.AntoxOnMessageCallback"
}

class AntoxOnMessageCallback(private var ctx: Context) extends FriendMessageCallback[Unit] {

  override def friendMessage(friendNumber: Int, messageType: ToxMessageType, timeDelta: Int, message: Array[Byte])(state: Unit): Unit = {
    MessageHelper.handleMessage(ctx, friendNumber,
      ToxSingleton.getAntoxFriend(friendNumber).get.getKey,
      new String(message, "UTF-8"), MessageType.fromToxMessageType(messageType))
  }
}
