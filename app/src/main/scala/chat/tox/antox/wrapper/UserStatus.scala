package chat.tox.antox.wrapper

import im.tox.tox4j.core.enums.ToxUserStatus


object UserStatus {

  def getToxUserStatusFromString(status: String): ToxUserStatus = {
    if (status == "online") return ToxUserStatus.NONE
    if (status == "away") return ToxUserStatus.AWAY
    if (status == "busy") return ToxUserStatus.BUSY
    ToxUserStatus.NONE
  }

  def getStringFromToxUserStatus(status: ToxUserStatus): String = {
    if (status == ToxUserStatus.NONE) return "online"
    if (status == ToxUserStatus.AWAY) return "away"
    if (status == ToxUserStatus.BUSY) return "busy"
    "invalid"
  }
}
