package im.tox.antox.utils

object Packets {

  val GROUP_INVITE_PACKET = 170
  val GROUP_ACCEPT_INVITE_PACKET: Byte = 171.asInstanceOf[Byte]
  val GROUP_PEER_JOIN_PACKET = 172
  val GROUP_PEER_LEAVE_PACKET = 173
  val GROUP_PEERLIST_CHANGE_PACKET = 174
}