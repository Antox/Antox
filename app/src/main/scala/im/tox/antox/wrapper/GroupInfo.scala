package im.tox.antox.wrapper

import java.sql.Timestamp

import im.tox.antox.utils.TimestampUtils

case class GroupInfo(key: String,
                     online: Boolean,
                     name: String,
                     topic: String,
                     favorite: Boolean,
                     lastMessage: String,
                     lastMessageTimestamp: Timestamp,
                     unreadCount: Int,
                     alias: String) extends ContactInfo {

  def statusMessage = topic
  val status = if (online) "online" else "offline"
  val receivedAvatar = true
  val avatar = None

  def this(id: String, online: Boolean, name: String, topic: String, favorite: Boolean, alias: String)  {
    this(id, online, name, topic, favorite, "", TimestampUtils.emptyTimestamp(), 0, alias)
  }
}
