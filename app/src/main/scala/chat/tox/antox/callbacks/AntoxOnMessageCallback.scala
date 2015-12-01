package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.MessageHelper
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.data.ToxFriendMessage
import im.tox.tox4j.core.enums.ToxMessageType

class AntoxOnMessageCallback(private var ctx: Context) {

  def friendMessage(friendInfo: FriendInfo, messageType: ToxMessageType, timeDelta: Int, message: ToxFriendMessage)(state: Unit): Unit = {
    MessageHelper.handleMessage(ctx, friendInfo, message, messageType)
  }
}
