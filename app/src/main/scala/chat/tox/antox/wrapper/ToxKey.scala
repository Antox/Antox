package chat.tox.antox.wrapper

import chat.tox.antox.utils.Hex

object ToxKey {
  val MAX_KEY_LENGTH = 64

  def isKeyValid(key: String): Boolean =
    key.length == MAX_KEY_LENGTH && key.matches("^[0-9A-F]+$")
}

trait ToxKey {

  def key: String

  if (!ToxKey.isKeyValid(key)) {
    throw new IllegalArgumentException(s"key must be ${ToxKey.MAX_KEY_LENGTH} hex chars long")
  }

  def bytes: Array[Byte] = Hex.hexStringToBytes(key)

  override def toString: String = key
}

trait ContactKey extends ToxKey
final case class SelfKey(key: String) extends ToxKey
final case class FriendKey(key: String) extends ContactKey
final case class GroupKey(key: String) extends ContactKey
final case class PeerKey(key: String) extends ContactKey