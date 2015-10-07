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
    messageType match {
      case ToxMessageType.ACTION =>
        ACTION
      case ToxMessageType.NORMAL =>
        MESSAGE
      case _ =>
        NONE
    }
  }

  def toToxMessageType(messageType: MessageType): ToxMessageType = {
    messageType match {
      case MessageType.ACTION | MessageType.GROUP_ACTION =>
        ToxMessageType.ACTION

      case MessageType.MESSAGE | MessageType.GROUP_MESSAGE =>
        ToxMessageType.NORMAL

      case _ =>
        throw new Exception("Invalid message type for conversion to ToxMessageType.")
    }
  }
}
