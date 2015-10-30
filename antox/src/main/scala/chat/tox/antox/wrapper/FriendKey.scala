package chat.tox.antox.wrapper

import chat.tox.antox.utils.Hex

case class FriendKey(key: String) extends ToxKey {
  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))
}
