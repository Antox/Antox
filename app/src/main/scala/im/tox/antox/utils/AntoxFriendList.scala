package im.tox.antox.utils

import java.util
import java.util.{ArrayList, Collections, List, Locale}

import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.enums.ToxStatus
import im.tox.tox4j.core.exceptions.ToxFriendAddException
import im.tox.tox4j.exceptions.ToxException

import scala.collection.JavaConversions._

class AntoxFriendList {

  private var friends: util.List[AntoxFriend] = Collections.synchronizedList(new util.ArrayList[AntoxFriend]())

  def this(friends: util.ArrayList[AntoxFriend]) {
    this()
    this.friends = friends
  }

  def getByFriendNumber(friendNumber: Int): Option[AntoxFriend] = {
    friends.find(friend => friend.getFriendnumber == friendNumber)
  }

  def getByKey(key: String): Option[AntoxFriend] = {
    friends.find(friend => friend.getKey == key)
  }

  def getByName(name: String, ignorecase: Boolean): util.List[AntoxFriend] = {
    if (ignorecase) {
      return getByNameIgnoreCase(name)
    } else {
      friends.filter(friend => (friend.name == null && name == null) || (name != null && name == friend.name))
    }
  }

  private def getByNameIgnoreCase(name: String): util.List[AntoxFriend] = {
    friends.filter(friend => (friend.name == null && name == null) || (name != null && name.equalsIgnoreCase(friend.name)))
  }

  def searchFriend(partial: String): util.List[AntoxFriend] = {
    val partialLowered = partial.toLowerCase(Locale.US)
    if (partial == null) {
      throw new IllegalArgumentException("Cannot search for null")
    }
    friends.filter(friend => (friend.name != null && friend.name.contains(partialLowered)))
  }

  def getByStatus(status: ToxStatus): util.List[AntoxFriend] = {
    friends.filter(friend => friend.isOnline && friend.getStatus == status)
  }

  def getOnlineFriends: util.List[AntoxFriend] = {
    friends.filter(friend => friend.isOnline)
  }

  def getOfflineFriends: util.List[AntoxFriend] = {
    friends.filter(friend => !friend.isOnline)
  }

  def all(): util.List[AntoxFriend] = {
    new util.ArrayList[AntoxFriend](this.friends)
  }

  def addFriend(friendnumber: Int): AntoxFriend = {
    friends.filter(friend => friend.getFriendnumber == friendnumber).headOption match {
      case Some(f) => throw new Exception()
      case None => {
        val f = new AntoxFriend(friendnumber)
        this.friends.add(f)
        f
      }
    }
  }

  def addFriendIfNotExists(friendnumber: Int): AntoxFriend = {
    friends.filter(friend => friend.getFriendnumber == friendnumber).headOption match {
      case Some(f) => f
      case None => {
        val f = new AntoxFriend(friendnumber)
        this.friends.add(f)
        f
      }
    }
  }

  def updateFromFriend(friend: Friend): Unit = {
    val antoxFriend = getByKey(friend.key).get
    antoxFriend.setName(friend.name)
    antoxFriend.setStatusMessage(friend.status)
    antoxFriend.setOnline(friend.isOnline)
  }

  def removeFriend(friendnumber: Int) {
    friends.remove(friends.find(friend => friend.getFriendnumber == friendnumber).get)
  }
}
