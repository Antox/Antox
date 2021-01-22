package chat.tox.antox.wrapper

import im.tox.tox4j.core.data.ToxFriendNumber

object CallNumber {
  def fromFriendNumber(friendNumber: ToxFriendNumber): CallNumber = {
    CallNumber(friendNumber.value)
  }
}

final case class CallNumber(value: Int) extends AnyVal
