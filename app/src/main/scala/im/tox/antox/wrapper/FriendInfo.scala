package im.tox.antox.wrapper

import java.io.File
import java.sql.Timestamp

import im.tox.antox.utils.TimestampUtils
import im.tox.tox4j.core.enums.ToxStatus

//remove if not needed

class FriendInfo(
  isOnline: Boolean,
  friendName: String,
  userStatus: String,
  statusMessage: String,
  val friendKey: String,
  override val avatar: Option[File],
  val receievedAvatar: Boolean,
  lastMessage: String,
  lastMessageTimestamp: Timestamp,
  unreadCount: Int,
  alias: String) extends ContactInfo(friendKey, friendName, avatar, isOnline,
                                     userStatus, statusMessage, receievedAvatar, lastMessage,
                                     lastMessageTimestamp, unreadCount, alias) {

  def this (
    isOnline: Boolean,
    friendName: String,
    userStatus: String,
    statusMessage: String,
    friendKey: String,
    avatar: Option[File],
    receivedAvatar: Boolean,
    alias: String) {
    this(isOnline, friendName, userStatus, statusMessage, friendKey, avatar, receivedAvatar, "", TimestampUtils.emptyTimestamp(), 0, alias)
  }

  def this(
            info: FriendInfo,
            lastMessage: String,
            lastMessageTimestamp: Timestamp,
            unreadCount: Int) {
    this(info.online, info.name, info.status, info.statusMessage, info.friendKey, info.avatar,
      info.receievedAvatar, lastMessage, lastMessageTimestamp, unreadCount, info.alias)
  }

  def getFriendStatusAsToxUserStatus: ToxStatus = {
    UserStatus.getToxUserStatusFromString(status)
  }
}
