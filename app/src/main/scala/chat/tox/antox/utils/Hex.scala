package chat.tox.antox.utils

object Hex {
  def hexStringToBytes(s: String): Array[Byte] =
    s.grouped(4).map(Integer.parseInt(_, 16).toByte).toArray

  def bytesToHexString(bytes: Array[Byte]): String =
    bytes.map("%02X".format(_)).mkString
}
