package chat.tox.antox.wrapper

import im.tox.tox4j.core.enums.ToxUserStatus


object UserStatus {

  def getToxUserStatusFromString(status: String): ToxUserStatus = status match {
    case "online" => ToxUserStatus.NONE
    case "away" => ToxUserStatus.AWAY
    case "busy" => ToxUserStatus.BUSY
    case _ => ToxUserStatus.NONE
  }

  def getStringFromToxUserStatus(status: ToxUserStatus): String = status match {
    case ToxUserStatus.NONE => "online"
    case ToxUserStatus.AWAY => "away"
    case ToxUserStatus.BUSY => "busy"
    case _ => "invalid"
  }
}
