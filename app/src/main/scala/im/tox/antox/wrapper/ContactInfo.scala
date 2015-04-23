package im.tox.antox.wrapper

import java.io.File
import java.sql.Timestamp

class ContactInfo(
  val key: String,
  var name: String,
  val avatar: Option[File],
  var online: Boolean,
  var status: String,
  var statusMessage: String,
  var receivedAvatar: Boolean,
  var lastMessage: String,
  var lastMessageTimestamp: Timestamp,
  var unreadCount: Int,
  var alias: String) {

  /**
  Returns 'alias' if it has been set, otherwise returns 'name'.
    */
  def getAliasOrName: String = {
    if (alias != "") alias else name
  }
}