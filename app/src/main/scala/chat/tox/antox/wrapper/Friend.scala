package chat.tox.antox.wrapper

import java.io.File
import java.util

import chat.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.enums.ToxUserStatus

import scala.beans.{BeanProperty, BooleanBeanProperty}

class Friend(friendNumber: Int) extends Contact {

  @BeanProperty
  var name: String = _

  @BeanProperty
  var key: ToxKey = _

  @BeanProperty
  var avatar: Option[File] = None

  @BeanProperty
  var status: ToxUserStatus = ToxUserStatus.NONE

  @BeanProperty
  var statusMessage: String = _

  @BooleanBeanProperty
  var online: Boolean = false

  var isTyping: Boolean = _

  @BeanProperty
  var nickname: String = _

  @BeanProperty
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
    setAvatar(None)
  }

  def setTyping(isTyping: Boolean) {
    this.isTyping = isTyping
  }

}
