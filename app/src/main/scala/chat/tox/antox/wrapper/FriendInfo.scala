package chat.tox.antox.wrapper

import java.io.File

import im.tox.tox4j.core.data.ToxNickname
import im.tox.tox4j.core.enums.ToxUserStatus

case class FriendInfo(
                       online: Boolean,
                       name: ToxNickname,
                       alias: Option[ToxNickname],
                       status: String,
                       statusMessage: String,
                       key: FriendKey,
                       avatar: Option[File],
                       receivedAvatar: Boolean,
                       blocked: Boolean,
                       ignored: Boolean,
                       favorite: Boolean,
                       lastMessage: Option[Message],
                       unreadCount: Int) extends ContactInfo {

  def this(
            online: Boolean,
            name: ToxNickname,
            alias: Option[ToxNickname],
            status: String,
            statusMessage: String,
            key: FriendKey,
            avatar: Option[File],
            receivedAvatar: Boolean,
            blocked: Boolean,
            ignored: Boolean,
            favorite: Boolean) {
    this(online, name, alias, status, statusMessage, key, avatar, receivedAvatar, blocked, ignored, favorite, None, 0)
  }

  def getFriendStatusAsToxUserStatus: ToxUserStatus = {
    UserStatus.getToxUserStatusFromString(status)
  }
}
