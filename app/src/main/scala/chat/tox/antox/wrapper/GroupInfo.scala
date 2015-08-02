package chat.tox.antox.wrapper

import java.sql.Timestamp

import chat.tox.antox.utils.TimestampUtils

case class GroupInfo(key: ToxKey,
                     online: Boolean,
                     name: String,
                     topic: String,
                     blocked: Boolean,
                     ignored: Boolean,
                     favorite: Boolean,
                     lastMessage: String,
                     lastMessageTimestamp: Timestamp,
                     unreadCount: Int,
                     alias: String) extends ContactInfo {

  def statusMessage = topic
  val status = if (online) "online" else "offline"
  val receivedAvatar = true
  val avatar = None

  def this(key: ToxKey, online: Boolean, name: String, topic: String,
           blocked: Boolean, ignored: Boolean, favorite: Boolean, alias: String)  {
    this(key, online, name, topic, blocked, ignored, favorite,
      "", TimestampUtils.emptyTimestamp(), 0, alias)
  }
}
