package chat.tox.antox.wrapper

import chat.tox.antox.utils.Hex

object ToxKey {
  val MAX_KEY_LENGTH = 64

  def isKeyValid(key: String): Boolean =
    !(key.length != MAX_KEY_LENGTH || key.matches("[[:xdigit:]]"))
}

case class ToxKey(key: String) {
  if (!ToxKey.isKeyValid(key)) {
    throw new IllegalArgumentException(s"key must be $ToxKey.MAX_KEY_LENGTH hex chars long")
  }

  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))

  def bytes = Hex.hexStringToBytes(key)

  override def toString: String = key
}
