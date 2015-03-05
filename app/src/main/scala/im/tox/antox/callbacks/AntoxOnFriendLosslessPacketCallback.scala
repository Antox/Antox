package im.tox.antox.callbacks

import android.content.Context
import im.tox.tox4j.core.callbacks.FriendLosslessPacketCallback

class AntoxOnFriendLosslessPacketCallback(ctx: Context) extends FriendLosslessPacketCallback {

  override def friendLosslessPacket(friendNumber: Int, data: Array[Byte]): Unit = {
    //Do nothing
  }
}
