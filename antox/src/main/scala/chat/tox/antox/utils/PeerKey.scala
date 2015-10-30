package chat.tox.antox.utils

import chat.tox.antox.wrapper.ToxKey

case class PeerKey(key: String) extends ToxKey {
  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))
}