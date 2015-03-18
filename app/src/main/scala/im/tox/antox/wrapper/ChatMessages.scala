package im.tox.antox.wrapper

import java.sql.Timestamp

import im.tox.antox.wrapper.MessageType.MessageType

//remove if not needed

class ChatMessages() {

  var id: Int = _
  var message_id: Int = _
  var key: String = _
  var sender_name: String = _
  var message: String = _
  var time: Timestamp = _
  var received: Boolean = _
  var sent: Boolean = _
  var size: Int = _
  var `type`: MessageType = _

  def isMine: Boolean = {
    `type` == MessageType.OWN || `type` == MessageType.FILE_TRANSFER || `type` == MessageType.GROUP_OWN
  }

  def getType: MessageType = `type`

  override def toString: String = message
}
