package chat.tox.antox.wrapper

import chat.tox.antox.utils.Hex

object ToxAddress {

  val MAX_ADDRESS_LENGTH = 76

  def isAddressValid(address: String): Boolean =
    address.length == MAX_ADDRESS_LENGTH &&
      address.matches("^[0-9A-F]+$") &&
      address.grouped(4).map(Integer.parseInt(_, 16)).fold(0)(_ ^ _) == 0


  def removePrefix(address: String): String = {
    val prefix = "tox:"

    if (address.toLowerCase.contains(prefix)) {
      address.substring(prefix.length)
    } else {
      address
    }
  }
}

case class ToxAddress(address: String) {
  def fixedAddress : String = ToxAddress.removePrefix(address.toUpperCase())
  if (!ToxAddress.isAddressValid(fixedAddress)) {
    throw new IllegalArgumentException(s"address must be $ToxAddress.MAX_ADDRESS_LENGTH hex chars long")
  }

  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))

  def bytes: Array[Byte] = Hex.hexStringToBytes(fixedAddress)

  def key: FriendKey = FriendKey(fixedAddress.substring(0, ToxKey.MAX_KEY_LENGTH))

  override def toString: String = fixedAddress
}
