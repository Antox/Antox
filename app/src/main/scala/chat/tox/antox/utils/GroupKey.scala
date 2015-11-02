package chat.tox.antox.utils

import chat.tox.antox.wrapper.{ContactKey, ToxKey}

case class GroupKey(key: String) extends ContactKey {
  def this(bytes: Array[Byte]) =
    this(Hex.bytesToHexString(bytes))
}