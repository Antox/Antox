package im.tox.antox.utils

import im.tox.jtoxcore.ToxUserStatus
//remove if not needed
import scala.collection.JavaConversions._

class Friend(
  val isOnline: Boolean,
  val friendName: String,
  val friendStatus: String,
  val personalNote: String,
  val friendKey: String,
  val alias: String) {

  def getFriendStatusAsToxUserStatus(): ToxUserStatus = {
    UserStatus.getToxUserStatusFromString(friendStatus)
  }

  override def toString(): String = friendName
}
