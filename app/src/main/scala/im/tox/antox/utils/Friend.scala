package im.tox.antox.utils

import im.tox.tox4j.core.enums.ToxStatus

//remove if not needed

class Friend(
  val isOnline: Boolean,
  val friendName: String,
  val friendStatus: String,
  val personalNote: String,
  val friendKey: String,
  val alias: String) {

  def getFriendStatusAsToxUserStatus(): ToxStatus = {
    UserStatus.getToxUserStatusFromString(friendStatus)
  }

  override def toString(): String = friendName
}
