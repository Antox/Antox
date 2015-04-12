package im.tox.antox.wrapper

import java.sql.Timestamp

import im.tox.antox.utils.TimestampUtils

//remove if not needed

class GroupInfo(
  val id: String,
  val connected: Boolean,
  name: String,
  val topic: String,
  lastMessage: String,
  lastMessageTimestamp: Timestamp,
  unreadCount: Int,
  alias: String) extends ContactInfo(id, name, None, connected, if (connected) "online" else "offline",
                                     topic, lastMessage, lastMessageTimestamp, unreadCount, alias) {

  def this(group: Group, lastMessage: String, lastMessageTimestamp: Timestamp, unreadCount: Int) {
    this(group.id, group.connected, group.name, group.topic, lastMessage, lastMessageTimestamp, unreadCount, group.alias)
  }

  def this(id: String, isConnected: Boolean, name: String, topic: String, alias: String)  {
    this(id, isConnected, name, topic, "", TimestampUtils.emptyTimestamp(), 0, alias)
  }
}
