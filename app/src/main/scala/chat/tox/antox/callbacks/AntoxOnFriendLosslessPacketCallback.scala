package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.data.ToxLosslessPacket

class AntoxOnFriendLosslessPacketCallback(ctx: Context) {

  def friendLosslessPacket(friendInfo: FriendInfo, data: ToxLosslessPacket)(state: Unit): Unit = {
    //Do nothing
  }
}
