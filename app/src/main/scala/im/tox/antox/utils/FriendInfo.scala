package im.tox.antox.utils

import java.sql.Timestamp
//remove if not needed

class FriendInfo(
  isOnline: Boolean,
  friendName: String,
  userStatus: String,
  personalNote: String,
  friendKey: String,
  val lastMessage: String,
  val lastMessageTimestamp: Timestamp,
  val unreadCount: Int,
  alias: String) extends Friend(isOnline, friendName, userStatus, personalNote, friendKey, alias) {

}
