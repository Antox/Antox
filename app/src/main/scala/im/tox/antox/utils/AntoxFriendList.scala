package im.tox.antox.utils

import java.util.{ArrayList, Collections, List, Locale}

import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.enums.ToxStatus
import im.tox.tox4j.core.exceptions.ToxFriendAddException
import im.tox.tox4j.exceptions.ToxException

//remove if not needed
import scala.collection.JavaConversions._

class AntoxFriendList {

  private var friends: List[AntoxFriend] = Collections.synchronizedList(new ArrayList[AntoxFriend]())

  def this(friends: ArrayList[AntoxFriend]) {
    this()
    this.friends = friends
  }

  def getByFriendNumber(friendNumber: Int): Option[AntoxFriend] = {
    friends.filter(friend => friend.getFriendnumber == friendNumber).headOption
  }

  def getByClientAddress(address: String): Option[AntoxFriend] = {
    friends.filter(friend => friend.getAddress == address).headOption
  }

  def getByClientId(id: String): Option[AntoxFriend] = {
    friends.filter(friend => friend.getClientId == id).headOption
  }

  def getByAddress(address: String): AntoxFriend = {
    getByClientAddress(address) match {
      case Some(x) => x
      case None => null
    }
  }

  def getByName(name: String, ignorecase: Boolean): List[AntoxFriend] = {
    if (ignorecase) {
      return getByNameIgnoreCase(name)
    } else {
      friends.filter(friend => (friend.name == null && name == null) || (name != null && name == friend.name))
    }
  }

  private def getByNameIgnoreCase(name: String): List[AntoxFriend] = {
    friends.filter(friend => (friend.name == null && name == null) || (name != null && name.equalsIgnoreCase(friend.name)))
  }

  def searchFriend(partial: String): List[AntoxFriend] = {
    val partialLowered = partial.toLowerCase(Locale.US)
    if (partial == null) {
      throw new IllegalArgumentException("Cannot search for null")
    }
    friends.filter(friend => (friend.name != null && friend.name.contains(partialLowered)))
  }

  def getByStatus(status: ToxStatus): List[AntoxFriend] = {
    friends.filter(friend => friend.isOnline && friend.getStatus == status)
  }

  def getOnlineFriends(): List[AntoxFriend] = {
    friends.filter(friend => friend.isOnline)
  }

  def getOfflineFriends(): List[AntoxFriend] = {
    friends.filter(friend => !friend.isOnline)
  }

  def all(): List[AntoxFriend] = {
    new ArrayList[AntoxFriend](this.friends)
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
    val antoxFriend = getByClientId(ToxSingleton.clientIdFromAddress(friend.friendKey)).get
    antoxFriend.setAddress(friend.friendKey)
    antoxFriend.setName(friend.friendName)
    antoxFriend.setStatusMessage(friend.friendStatus)
    antoxFriend.setOnline(friend.isOnline)
  }

  def removeFriend(friendnumber: Int) {
    friends.remove(friends.find(friend => friend.getFriendnumber == friendnumber))
  }
}
