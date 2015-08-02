package chat.tox.antox.wrapper

import chat.tox.antox.utils.Hex

object ToxAddress {
  val MAX_ADDRESS_LENGTH = 76

  def isAddressValid(address: String): Boolean = {
    if (address.length != MAX_ADDRESS_LENGTH || address.matches("[[:xdigit:]]")) {
      return false
    }

    var x = 0
    try {
      var i = 0
      while (i < address.length) {
        x = x ^
          java.lang.Integer.valueOf(address.substring(i, i + 4), 16)
        i += 4
      }
    } catch {
      case e: NumberFormatException => return false
    }
    x == 0
  }
}

case class ToxAddress(address: String) {
  if (!ToxAddress.isAddressValid(address)) {
    throw new IllegalArgumentException(s"address must be $ToxAddress.MAX_ADDRESS_LENGTH hex chars long")
  }

  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))

  def bytes = Hex.hexStringToBytes(address)
  def key = new ToxKey(address.substring(0, ToxKey.MAX_KEY_LENGTH))

  override def toString: String = address
}
