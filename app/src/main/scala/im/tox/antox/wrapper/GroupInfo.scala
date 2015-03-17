package im.tox.antox.wrapper

import java.sql.Timestamp

import im.tox.antox.utils.TimestampUtils

//remove if not needed

class GroupInfo(
  val id: String,
  val connected: Boolean,
  val name: String,
  val topic: String,
  var lastMessage: String,
  var lastMessageTimestamp: Timestamp,
  var unreadCount: Int,
  alias: String) {

  def this(group: Group, lastMessage: String, lastMessageTimestamp: Timestamp, unreadCount: Int) {
    this(group.id, group.connected, group.name, group.topic, lastMessage, lastMessageTimestamp, unreadCount, group.alias)
  }

  def this(id: String, isConnected: Boolean, name: String, topic: String, alias: String)  {
    this(id, isConnected, name, topic, "", TimestampUtils.emptyTimestamp(), 0, alias)
  }

  /**
  Returns 'alias' if it has been set, otherwise returns 'name'.
   */
  def getAliasOrName(): String = {
    if (alias != "") alias else name
  }
}
