package chat.tox.antox.utils

import chat.tox.antox.wrapper.ToxKey

/**
 * ToxKey subclass used to represent raw public keys, rather than identifying tox entities.
 */
case class ToxPublicKey(key: String) extends ToxKey {
  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))
}
