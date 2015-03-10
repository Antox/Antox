package im.tox.antox.wrapper

import java.util
import java.util.{Collections, Locale}

//remove if not needed
import scala.collection.JavaConversions._

class GroupList {

  private var groups: util.List[Group] = new util.ArrayList[Group]()

  def this(groups: util.List[Group]) {
    this()
    this.groups = groups
  }

  def getGroup(groupNumber: Int): Group = {
    groups.find(group => group.groupNumber == groupNumber).get
  }

  def getGroup(id: String): Group = {
    groups.find(group => group.id == id).get
  }

  def getPeer(groupNumber: Int, peerNumber: Int): GroupPeer = {
    getGroup(groupNumber).peers.getPeer(peerNumber)
  }

  def getByTitle(title: String, ignorecase: Boolean): util.List[Group] = {
    if (ignorecase) {
      return getByTitleIgnoreCase(title)
    } else {
      groups.filter(group => (group.name == null && title == null) || (title != null && title == group.name))
    }
  }

  private def getByTitleIgnoreCase(title: String): util.List[Group] = {
    groups.filter(group => (group.name == null && title == null) || (title != null && title.equalsIgnoreCase(group.name)))
  }

  def searchGroup(partial: String): util.List[Group] = {
    val partialLowered = partial.toLowerCase(Locale.getDefault)
    if (partial == null) {
      throw new IllegalArgumentException("Cannot search for null")
    }
    groups.filter(group => group.name != null && group.name.contains(partialLowered))
  }

  def all(): util.List[Group] = {
    new util.ArrayList[Group](this.groups)
  }

  def addGroup(group: Group): Unit = {
    println("group " + group.groupNumber + " added")
    groups.find(existingGroup => existingGroup.groupNumber == group.groupNumber) match {
      case Some(f) => throw new Exception()
      case None => this.groups.add(group)
    }
  }

  def addGroupIfNotExists(group: Group): Unit = {
    groups.find(existingGroup => existingGroup.groupNumber == group.groupNumber).headOption match {
      case Some(f) => return
      case None =>
        this.groups.add(group)
    }
  }

  def removeGroup(groupNumber: Int) {
    println("before remove")
    for (group <- groups) {
      println("group " + group.name)
    }
    groups.remove(groups.find(group => group.groupNumber == groupNumber))
    println("after remove")
    for (group <- groups) {
      println("group " + group.name)
    }
  }
}
