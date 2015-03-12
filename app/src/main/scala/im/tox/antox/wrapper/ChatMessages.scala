package im.tox.antox.wrapper

import java.sql.Timestamp
//remove if not needed

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
  val `type`: Int) {

  def isMine: Boolean = {
    if (`type` == 1 || `type` == 3) true else false
  }

  def getType: Int = `type`

  override def toString: String = message
}
