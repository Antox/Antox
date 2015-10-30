package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.callbacks.FriendLosslessPacketCallback

class AntoxOnFriendLosslessPacketCallback(ctx: Context) {

  def friendLosslessPacket(friendInfo: FriendInfo, data: Array[Byte])(state: Unit): Unit = {
    //Do nothing
  }
}
