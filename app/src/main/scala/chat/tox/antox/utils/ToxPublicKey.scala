package chat.tox.antox.utils

import chat.tox.antox.wrapper.ToxKey

/**
 * Used to represent raw public keys, rather than identifying tox entities.
 */
case class ToxPublicKey(key: String) {

  def bytes: Array[Byte] = Hex.hexStringToBytes(key)
}
