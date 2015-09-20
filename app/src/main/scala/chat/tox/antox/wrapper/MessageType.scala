package chat.tox.antox.wrapper

import im.tox.tox4j.core.enums.ToxMessageType

//Don't change this order (it will break the DB)
object MessageType extends Enumeration {
  type MessageType = Value
  val NONE = Value("0")
  val MESSAGE = Value("1")
  val FILE_TRANSFER = Value("2")
  val ACTION = Value("3")
  val GROUP_MESSAGE = Value("4")
  val GROUP_ACTION = Value("5")

  val transferValues = Set(FILE_TRANSFER)

  def fromToxMessageType(messageType: ToxMessageType): MessageType = {
    if (messageType == ToxMessageType.ACTION) {
      ACTION
    } else if (messageType == ToxMessageType.NORMAL) {
      MESSAGE
    } else {
      NONE
    }
  }
}
