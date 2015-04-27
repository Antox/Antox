package im.tox.antox.wrapper

import im.tox.tox4j.core.enums.ToxMessageType

//Don't change this order (it will break the DB)
object MessageType extends Enumeration {
  type MessageType = Value
  val NONE = Value(0)
  val OWN = Value(1)
  val FRIEND = Value(2)
  val FILE_TRANSFER = Value(3)
  val FILE_TRANSFER_FRIEND = Value(4)
  val ACTION = Value(5)
  val GROUP_OWN = Value(6)
  val GROUP_PEER = Value(7)
  val GROUP_ACTION = Value(8)

  def fromToxMessageType(messageType: ToxMessageType): MessageType = {
    if (messageType == ToxMessageType.ACTION) {
      ACTION
    } else if (messageType == ToxMessageType.NORMAL) {
      FRIEND
    } else {
      NONE
    }
  }
}
