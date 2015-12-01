package chat.tox.antox.utils

import chat.tox.antox.wrapper.ContactKey

case class PeerKey(key: String) extends ContactKey {
  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))
}