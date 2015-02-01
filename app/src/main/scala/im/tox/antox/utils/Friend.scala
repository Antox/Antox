package im.tox.antox.utils

import im.tox.tox4j.core.enums.ToxStatus

class Friend(
  val isOnline: Boolean,
  val name: String,
  val status: String,
  val statusMessage: String,
  val key: String,
  val alias: String) {

  def getFriendStatusAsToxUserStatus: ToxStatus = {
    UserStatus.getToxUserStatusFromString(status)
  }

  override def toString: String = name
}
