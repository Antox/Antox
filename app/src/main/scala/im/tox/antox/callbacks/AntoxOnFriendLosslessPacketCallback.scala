package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.utils.{Packets, Hex}
import im.tox.tox4j.core.callbacks.FriendLosslessPacketCallback

class AntoxOnFriendLosslessPacketCallback(ctx: Context) extends FriendLosslessPacketCallback {

  override def friendLosslessPacket(friendNumber: Int, data: Array[Byte]): Unit = {
    val packetNumber = data(0) & 0xFF
    println("got a packet from " + friendNumber + " of type " + packetNumber)
    val remainingData = data.drop(1)
    packetNumber match {
      case Packets.GROUP_INVITE_PACKET =>
        new AntoxOnGroupInviteCallback(ctx).groupInvite(friendNumber,
          Hex.hexStringToBytes(new String(remainingData)),
          Array(xs = friendNumber.asInstanceOf[Byte]))
      case Packets.GROUP_PEER_JOIN_PACKET =>
        new AntoxOnGroupPeerJoinCallback(ctx).groupPeerJoin(remainingData(0),
          remainingData(1))
    }
  }

  def convert(buf: Array[Byte]): Array[Int] = {
    val intArray = new Array[Int](buf.length / 4)
    var offset = 0
    for(i <- intArray) {
      intArray(i) = (buf(3 + offset) & 0xFF) | ((buf(2 + offset) & 0xFF) << 8) |
      ((buf(1 + offset) & 0xFF) << 16) | ((buf(0 + offset) & 0xFF) << 24);
      offset += 4
    }
    intArray
  }
}
