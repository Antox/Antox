package chat.tox.antox.wrapper

import java.sql.Timestamp

import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.TimestampUtils
import chat.tox.antox.wrapper.MessageType.MessageType


class Message(
  val id: Int,
  val message_id: Int,
  val key: ToxKey,
  val sender_name: String,
  val message: String,
  val received: Boolean,
  val has_been_read: Boolean,
  val sent: Boolean,
  val timestamp: Timestamp,
  val size: Int,
  val `type`: MessageType,
  val fileKind: FileKind) {

  def logFormat(): Option[String] = {
    if (this.isFileTransfer) return None

    val name = if (`type` == MessageType.OWN) {
      ToxSingleton.tox.getName
    } else {
      ToxSingleton.getAntoxFriend(key).get.name
    }
    Some("<" + name + "> " +
      message + "  [" + TimestampUtils.prettyTimestamp(timestamp, isChat = true) + "]")
  }

  def isMine: Boolean = {
    `type` == MessageType.OWN || `type` == MessageType.FILE_TRANSFER || `type` == MessageType.GROUP_OWN
  }

  def isFileTransfer: Boolean = {
    `type` == MessageType.FILE_TRANSFER || `type` == MessageType.FILE_TRANSFER_FRIEND
  }
}
