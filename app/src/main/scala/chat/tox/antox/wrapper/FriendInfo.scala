package chat.tox.antox.wrapper

import java.io.File
import java.sql.Timestamp

import chat.tox.antox.utils.TimestampUtils
import im.tox.tox4j.core.enums.ToxUserStatus

case class FriendInfo(
  online: Boolean,
  name: String,
  status: String,
  statusMessage: String,
  key: ToxKey,
  avatar: Option[File],
  receivedAvatar: Boolean,
  blocked: Boolean,
  ignored: Boolean,
  favorite: Boolean,
  lastMessage: String,
  lastMessageTimestamp: Timestamp,
  unreadCount: Int,
  alias: String) extends ContactInfo {

  def this (
    online: Boolean,
    name: String,
    status: String,
    statusMessage: String,
    key: ToxKey,
    avatar: Option[File],
    receivedAvatar: Boolean,
    blocked: Boolean,
    ignored: Boolean,
    favorite: Boolean,
    alias: String) {
    this(online, name, status, statusMessage, key, avatar, receivedAvatar, blocked, ignored, favorite, "", TimestampUtils.emptyTimestamp(), 0, alias)
  }

  def getFriendStatusAsToxUserStatus: ToxUserStatus = {
    UserStatus.getToxUserStatusFromString(status)
  }
}
