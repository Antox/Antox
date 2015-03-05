package im.tox.antox.wrapper

import java.sql.Timestamp

import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{Constants, PrettyTimestamp}

//remove if not needed

class Message(
  val id: Int,
  val message_id: Int,
  val key: String,
  val sender_name: String,
  val message: String,
  val has_been_received: Boolean,
  val has_been_read: Boolean,
  val successfully_sent: Boolean,
  val timestamp: Timestamp,
  val size: Int,
  val `type`: Int) {

  def logFormat(): Option[String] = {
    //TODO hack that will be fixed with groupchat db
    if (this.isFileTransfer) return None

    val name = if (`type` == Constants.MESSAGE_TYPE_OWN) {
      ToxSingleton.tox.getName
    } else {
      ToxSingleton.getAntoxFriend(key).get.name
    }
    Some("<" + name + "> " +
      message + "  [" + PrettyTimestamp.prettyTimestamp(timestamp, isChat = true) + "]")
  }

  def isFileTransfer: Boolean = {
    `type` == Constants.MESSAGE_TYPE_FILE_TRANSFER || `type` == Constants.MESSAGE_TYPE_FILE_TRANSFER_FRIEND
  }
}
