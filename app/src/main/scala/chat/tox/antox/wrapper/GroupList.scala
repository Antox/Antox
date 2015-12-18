package chat.tox.antox.wrapper

import java.util
import java.util.Locale

import chat.tox.antox.utils.{AntoxLog, UiUtils}

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

  def getGroup(key: ContactKey): Group = {
    groups.find(group => group.key == key).get
  }

  def getPeer(groupNumber: Int, peerNumber: Int): GroupPeer = {
    getGroup(groupNumber).peers.getPeer(peerNumber)
  }

  def getByTitle(title: String, ignorecase: Boolean): util.List[Group] = {
    if (ignorecase) {
      getByTitleIgnoreCase(title)
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
    AntoxLog.debug("group " + group.groupNumber + " added")
    groups.find(existingGroup => existingGroup.groupNumber == group.groupNumber) match {
      case Some(f) => throw new Exception()
      case None => this.groups.add(group)
    }
  }

  def addGroup(tox: ToxCore, groupNumber: Int): Unit = {
    addGroup(new Group(tox.getGroupKey(groupNumber),
      groupNumber, UiUtils.trimId(tox.getGroupKey(groupNumber)),
      "", "", new PeerList()))

  }

  def addGroupIfNotExists(group: Group): Unit = {
    groups.find(existingGroup => existingGroup.groupNumber == group.groupNumber) match {
      case Some(f) =>
      case None =>
        this.groups.add(group)
    }
  }

  def removeGroup(groupKey: GroupKey) {
    groups.remove(groups.find(group => group.key == groupKey).get)
  }
}
