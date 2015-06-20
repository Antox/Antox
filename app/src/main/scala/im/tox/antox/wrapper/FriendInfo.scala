package im.tox.antox.wrapper

import java.io.File
import java.sql.Timestamp

import im.tox.antox.utils.TimestampUtils
import im.tox.tox4j.core.enums.ToxStatus

case class FriendInfo(
  online: Boolean,
  name: String,
  status: String,
  statusMessage: String,
  key: String,
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
    isOnline: Boolean,
    friendName: String,
    userStatus: String,
    statusMessage: String,
    friendKey: String,
    avatar: Option[File],
    receivedAvatar: Boolean,
    blocked: Boolean,
    ignored: Boolean,
    favorite: Boolean,
    alias: String) {
    this(isOnline, friendName, userStatus, statusMessage, friendKey, avatar, receivedAvatar, blocked, ignored, favorite, "", TimestampUtils.emptyTimestamp(), 0, alias)
  }

  def getFriendStatusAsToxUserStatus: ToxStatus = {
    UserStatus.getToxUserStatusFromString(status)
  }
}
