package im.tox.antox.wrapper

import java.sql.Timestamp

import im.tox.antox.wrapper.MessageType.MessageType

class ChatMessages(
  val id: Int,
  val message_id: Int,
  val key: String,
  val sender_name: String,
  val message: String,
  val time: Timestamp,
  val received: Boolean,
  val sent: Boolean,
  val size: Int,
  val `type`: MessageType,
  val fileKind: FileKind) {

  def isMine: Boolean = {
    `type` == MessageType.OWN || `type` == MessageType.FILE_TRANSFER || `type` == MessageType.GROUP_OWN
  }

  def getType: MessageType = `type`

  override def toString: String = message
}
