package im.tox.antox.utils

import im.tox.jtoxcore.ToxUserStatus
//remove if not needed
import scala.collection.JavaConversions._

object UserStatus {

  def getToxUserStatusFromString(status: String): ToxUserStatus = {
    if (status == "online") return ToxUserStatus.TOX_USERSTATUS_NONE
    if (status == "away") return ToxUserStatus.TOX_USERSTATUS_AWAY
    if (status == "busy") return ToxUserStatus.TOX_USERSTATUS_BUSY
    ToxUserStatus.TOX_USERSTATUS_NONE
  }

  def getStringFromToxUserStatus(status: ToxUserStatus): String = {
    if (status == ToxUserStatus.TOX_USERSTATUS_NONE) return "online"
    if (status == ToxUserStatus.TOX_USERSTATUS_AWAY) return "away"
    if (status == ToxUserStatus.TOX_USERSTATUS_BUSY) return "busy"
    "invalid"
  }
}
