package im.tox.antox.wrapper

import java.sql.Timestamp
//remove if not needed

class GroupInfo(
  val id: String,
  val name: String,
  val topic: String,
  val lastMessage: String,
  val lastMessageTimestamp: Timestamp,
  val unreadCount: Int,
  alias: String) {

  def this(group: Group, lastMessage: String, lastMessageTimestamp: Timestamp, unreadCount: Int) {
    this(group.id, group.name, group.topic, lastMessage, lastMessageTimestamp, unreadCount, group.alias)
  }

  /**
  Returns 'alias' if it has been set, otherwise returns 'name'.
   */
  def getAliasOrName(): String = {
    if (alias != "") alias else name
  }
}
