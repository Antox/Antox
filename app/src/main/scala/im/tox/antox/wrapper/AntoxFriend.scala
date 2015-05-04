package im.tox.antox.wrapper

import java.io.File
import java.util

import im.tox.antox.av.Call
import im.tox.tox4j.core.enums.ToxStatus

import scala.beans.{BeanProperty, BooleanBeanProperty}

class AntoxFriend(_friendnumber: Int) {

  @transient private val friendNumber: Int = _friendnumber

  @BeanProperty
  var name: String = _

  @BeanProperty
  var key: String = _

  @BeanProperty
  var avatar: Option[File] = None

  @BeanProperty
  var status: ToxStatus = ToxStatus.NONE

  @BeanProperty
  var statusMessage: String = _

  @BooleanBeanProperty
  var online: Boolean = false

  var isTyping: Boolean = _

  @BeanProperty
  var nickname: String = _

  @BeanProperty
  var previousNames: util.ArrayList[String] = _

  val call = new Call(friendNumber)

  def getFriendnumber: Int = this.friendNumber

  def deleteAvatar(): Unit = {
    avatar.map(_.delete())
    setAvatar(None)
  }

  def setTyping(isTyping: Boolean) {
    this.isTyping = isTyping
  }
}
