package chat.tox.antox.wrapper

import java.io.File
import java.sql.Timestamp

trait ContactInfo {
  def key: ContactKey
  def name: String
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
  def alias: String

  /**
  Returns 'alias' if it has been set, otherwise returns 'name'.
    */
  def getAliasOrName: String = {
    if (alias != "") alias else name
  }
}