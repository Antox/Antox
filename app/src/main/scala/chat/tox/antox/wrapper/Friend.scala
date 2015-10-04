package chat.tox.antox.wrapper

import java.io.File
import java.util

import chat.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.enums.ToxUserStatus

import scala.beans.{BeanProperty, BooleanBeanProperty}

class Friend(friendNumber: Int) extends Contact {

  var name: String = _

  var key: ToxKey = _

  var avatar: Option[File] = None

  var status: ToxUserStatus = ToxUserStatus.NONE

  var statusMessage: String = _

  var online: Boolean = false

  var isTyping: Boolean = _

  var nickname: String = _

  var previousNames: util.ArrayList[String] = _

  def getFriendNumber: Int = this.friendNumber

  override def sendAction(action: String): Int = {
    ToxSingleton.tox.sendAction(friendNumber, action)
  }

  override def sendMessage(message: String): Int = {
    ToxSingleton.tox.sendMessage(friendNumber, message)
  }

  def deleteAvatar(): Unit = {
    avatar.map(_.delete())
    avatar = None
  }

  def setTyping(isTyping: Boolean) {
    this.isTyping = isTyping
  }

}
