package im.tox.antox.wrapper

import java.util
import java.util.{Collections, Locale}

import im.tox.tox4j.core.enums.ToxStatus

import scala.collection.JavaConversions._

class AntoxFriendList {

  private var friends: util.List[Friend] = Collections.synchronizedList(new util.ArrayList[Friend]())

  def this(friends: util.ArrayList[Friend]) {
    this()
    this.friends = friends
  }

  def getByFriendNumber(friendNumber: Int): Option[Friend] = {
    friends.find(friend => friend.getFriendNumber == friendNumber)
  }

  def getByKey(key: String): Option[Friend] = {
    friends.find(friend => friend.getKey == key)
  }

  def getByName(name: String, ignorecase: Boolean): util.List[Friend] = {
    if (ignorecase) {
      getByNameIgnoreCase(name)
    } else {
      friends.filter(friend => (friend.name == null && name == null) || (name != null && name == friend.name))
    }
  }

  private def getByNameIgnoreCase(name: String): util.List[Friend] = {
    friends.filter(friend => (friend.name == null && name == null) || (name != null && name.equalsIgnoreCase(friend.name)))
  }

  def searchFriend(partial: String): util.List[Friend] = {
    val partialLowered = partial.toLowerCase(Locale.getDefault)
    if (partial == null) {
      throw new IllegalArgumentException("Cannot search for null")
    }
    friends.filter(friend => friend.name != null && friend.name.contains(partialLowered))
  }

  def getOnlineFriends: util.List[Friend] = {
    friends.filter(friend => friend.isOnline)
  }

  def getOfflineFriends: util.List[Friend] = {
    friends.filter(friend => !friend.isOnline)
  }

  def all(): util.List[Friend] = {
    new util.ArrayList[Friend](this.friends)
  }

  def addFriend(friendnumber: Int): Friend = {
    friends.filter(friend => friend.getFriendNumber == friendnumber).headOption match {
      case Some(f) => throw new Exception()
      case None =>
        val f = new Friend(friendnumber)
        this.friends.add(f)
        f
    }
  }

  def addFriendIfNotExists(friendnumber: Int): Friend = {
    friends.filter(friend => friend.getFriendNumber == friendnumber).headOption match {
      case Some(f) => f
      case None =>
        val f = new Friend(friendnumber)
        this.friends.add(f)
        f
    }
  }

  def updateFromFriend(friend: FriendInfo): Unit = {
    val antoxFriend = getByKey(friend.key).get
    antoxFriend.setName(friend.name)
    antoxFriend.setStatusMessage(friend.status)
    antoxFriend.setOnline(friend.online)
  }

  def removeFriend(friendnumber: Int) {
    friends.remove(friends.find(friend => friend.getFriendNumber == friendnumber).get)
  }
}
