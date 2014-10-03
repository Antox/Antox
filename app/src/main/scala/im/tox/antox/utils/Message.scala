package im.tox.antox.utils

import java.sql.Timestamp
//remove if not needed
import scala.collection.JavaConversions._

class Message(
  val id: Int,
  val message_id: Int,
  val key: String,
  val message: String,
  val has_been_received: Boolean,
  val has_been_read: Boolean,
  val successfully_sent: Boolean,
  val timestamp: Timestamp,
  val size: Int,
  val `type`: Int) {
}
