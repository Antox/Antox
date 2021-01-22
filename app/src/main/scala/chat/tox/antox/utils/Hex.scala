package chat.tox.antox.utils

object Hex {
  def hexStringToBytes(s: String): Array[Byte] =
    s.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  def bytesToHexString(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString
}
