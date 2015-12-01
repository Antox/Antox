package chat.tox.antox.wrapper

import java.io.File

import chat.tox.antox.utils.UiUtils
import im.tox.tox4j.core.data.ToxNickname

trait ContactInfo {
  def key: ContactKey
  def name: ToxNickname
  def avatar: Option[File]
  def online: Boolean
  def status: String
  def statusMessage: String
  def receivedAvatar: Boolean
  def blocked: Boolean
  def ignored: Boolean
  def favorite: Boolean
  def lastMessage: Option[Message]
  def unreadCount: Int
  def alias: Option[ToxNickname]

  /**
   * Returns 'alias' if it has been set, otherwise returns 'name'.
   * If name is empty returns a segment of ID.
   */
  def getDisplayName: String = {
    val nameOrAlias = new String(alias.getOrElse(name).value)
    if (nameOrAlias.isEmpty) UiUtils.trimId(key) else nameOrAlias
  }
}