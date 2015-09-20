package chat.tox.antox.wrapper

import java.sql.Timestamp

import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.TimestampUtils
import chat.tox.antox.wrapper.MessageType.MessageType


case class Message(id: Int,
                   messageId: Int,
                   key: ToxKey,
                   senderKey: ToxKey,
                   senderName: String,
                   message: String,
                   received: Boolean,
                   read: Boolean,
                   sent: Boolean,
                   timestamp: Timestamp,
                   size: Int,
                   `type`: MessageType,
                   fileKind: FileKind) {

  def logFormat(): Option[String] = {
    if (this.isFileTransfer) return None

    val name =
      if (isMine) {
        ToxSingleton.tox.getName
      } else {
        ToxSingleton.getAntoxFriend(key).get.name
      }

    val prettyTimestamp = TimestampUtils.prettyTimestamp(timestamp, isChat = true)

    Some(s"<$name> $message [$prettyTimestamp]")
  }

  def isMine: Boolean = {
    senderKey.equals(ToxSingleton.tox.getSelfKey)
  }

  def isFileTransfer: Boolean = {
    MessageType.transferValues.contains(`type`)
  }
}
