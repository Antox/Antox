package im.tox.antox.wrapper

import im.tox.antox.tox.ToxSingleton

import scala.collection.JavaConversions._

class Group(val key: String,
            val groupNumber: Int,
            private var _name: String,
            var alias: String,
            var topic: String,
            val peers: PeerList) extends Contact {

  var connected = false


  override def sendAction(action: String): Int = {
    ToxSingleton.tox.sendGroupAction(groupNumber, action)
    0 //groupchats don't support receipts yet
  }

  override def sendMessage(message: String): Int = {
    ToxSingleton.tox.sendGroupMessage(groupNumber, message)
    0 //groupchats don't support receipts yet
  }

  def addPeer(tox: ToxCore, peerNumber: Int): Unit = {
    var peerName = tox.getGroupPeerName(groupNumber, peerNumber)
    if (peerName == null) peerName = ""
    this.peers.addGroupPeer(new GroupPeer(peerName, ignored = false))
    printPeerList()
  }

  def printPeerList(): Unit = {
    var number = 0
    for (peer <- peers.all()) {
      number += 1
    }
  }

  def getPeerCount: Int = {
    peers.all().size()
  }

  def clearPeerList(): Unit = {
    peers.clear()
  }

  def leave(partMessage: String): Unit = {
    ToxSingleton.tox.deleteGroup(groupNumber, partMessage)
  }

  override def toString: String = name

  //Getters
  def name = _name

  //Setters
  def name_= (name: String): Unit = {
    _name = name
  }
}
