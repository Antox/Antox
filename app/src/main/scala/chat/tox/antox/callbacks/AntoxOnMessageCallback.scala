package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.wrapper.{FriendInfo, MessageType}
import im.tox.tox4j.core.callbacks.FriendMessageCallback
import im.tox.tox4j.core.enums.ToxMessageType

class AntoxOnMessageCallback(private var ctx: Context) {

  def friendMessage(friendInfo: FriendInfo, messageType: ToxMessageType, timeDelta: Int, message: Array[Byte])(state: Unit): Unit = {
    MessageHelper.handleMessage(ctx, friendInfo, new String(message, "UTF-8"), messageType)
  }
}
