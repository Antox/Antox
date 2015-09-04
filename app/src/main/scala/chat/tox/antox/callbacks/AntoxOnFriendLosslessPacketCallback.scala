package chat.tox.antox.callbacks

import android.content.Context
import im.tox.tox4j.core.callbacks.FriendLosslessPacketCallback

class AntoxOnFriendLosslessPacketCallback(ctx: Context) extends FriendLosslessPacketCallback[Unit] {

  override def friendLosslessPacket(friendNumber: Int, data: Array[Byte])(state: Unit): Unit = {
    //Do nothing
  }
}
