package im.tox.antox.wrapper

import im.tox.tox4j.core.enums.ToxStatus


object UserStatus {

  def getToxUserStatusFromString(status: String): ToxStatus = {
    if (status == "online") return ToxStatus.NONE
    if (status == "away") return ToxStatus.AWAY
    if (status == "busy") return ToxStatus.BUSY
    ToxStatus.NONE
  }

  def getStringFromToxUserStatus(status: ToxStatus): String = {
    if (status == ToxStatus.NONE) return "online"
    if (status == ToxStatus.AWAY) return "away"
    if (status == ToxStatus.BUSY) return "busy"
    "invalid"
  }
}
