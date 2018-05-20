package chat.tox.antox.wrapper

import im.tox.tox4j.core.data.ToxNickname

case class GroupInfo(key: GroupKey,
                     online: Boolean,
                     name: ToxNickname,
                     alias: Option[ToxNickname],
                     topic: String,
                     blocked: Boolean,
                     ignored: Boolean,
                     favorite: Boolean,
                     lastMessage: Option[Message] = None,
                     unreadCount: Int = 0) extends ContactInfo {

  def statusMessage: String = topic

  val status = if (online) "online" else "offline"
  val receivedAvatar = true
  val avatar = None
}
