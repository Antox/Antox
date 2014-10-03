package im.tox.antox.utils

import java.sql.Timestamp
//remove if not needed
import scala.collection.JavaConversions._

class ChatMessages(
  val id: Int,
  val message_id: Int,
  val message: String,
  val time: Timestamp,
  val received: Boolean,
  val sent: Boolean,
  val size: Int,
  val `type`: Int) {

  def isMine(): Boolean = {
    if (`type` == 1 || `type` == 3) true else false
  }

  def getType(): Int = `type`
}
