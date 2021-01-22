package chat.tox.antox.wrapper

import chat.tox.antox.tox.ToxSingleton

import scala.collection.JavaConversions._

class Group(val key: GroupKey,
            val groupNumber: Int,
            var name: String,
            var alias: String,
            var topic: String,
            val peers: PeerList) {

  var connected = false

  def addPeer(tox: ToxCore, peerNumber: Int): Unit = {
    val peerKey = tox.getGroupPeerPublicKey(key, peerNumber)
    var peerName = tox.getGroupPeerName(key, peerNumber)
    if (peerName == null) peerName = ""
    //    this.peers.addGroupPeer(new GroupPeer(peerKey, peerName, ignored = false))
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
    ToxSingleton.tox.deleteGroup(key, partMessage)
  }

  override def toString: String = name
}
