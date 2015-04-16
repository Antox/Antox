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
  lastMessage: String,
  lastMessageTimestamp: Timestamp,
  unreadCount: Int,
  alias: String) extends ContactInfo(friendKey, friendName, avatar, isOnline,
                                     userStatus, statusMessage, lastMessage,
                                     lastMessageTimestamp, unreadCount, alias) {

  def this (
    isOnline: Boolean,
    friendName: String,
    userStatus: String,
    statusMessage: String,
    friendKey: String,
    avatar: Option[File],
    alias: String) {
    this(isOnline, friendName, userStatus, statusMessage, friendKey, avatar, "", TimestampUtils.emptyTimestamp(), 0, alias)
  }

  def getFriendStatusAsToxUserStatus: ToxStatus = {
    UserStatus.getToxUserStatusFromString(status)
  }
}
