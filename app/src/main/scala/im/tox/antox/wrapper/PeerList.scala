package im.tox.antox.wrapper

import java.util
import java.util.Collections

//remove if not needed
import scala.collection.JavaConversions._

class PeerList {

  private var peers: util.List[GroupPeer] = Collections.synchronizedList(new util.ArrayList[GroupPeer]())

  def this(peers: util.ArrayList[GroupPeer]) {
    this()
    this.peers = peers
  }

  def getByGroupPeerNumber(peerNumber: Int): Option[GroupPeer] = {
    peers.find(peer => peer.peerNumber == peerNumber)
  }

  def all(): util.List[GroupPeer] = {
    new util.ArrayList[GroupPeer](this.peers)
  }

  def addGroupPeer(peer: GroupPeer): Unit = {
    peers.find(existingGroupPeer => existingGroupPeer.peerNumber == peer.peerNumber) match {
      case Some(f) => throw new Exception()
      case None => this.peers.add(peer)
    }
  }

  def addGroupPeerIfNotExists(peer: GroupPeer): Unit = {
    peers.find(existingGroupPeer => existingGroupPeer.peerNumber == peer.peerNumber).headOption match {
      case Some(f) => return
      case None =>
        this.peers.add(peer)
    }
  }

  def removeGroupPeer(peerNumber: Int) {
    peers.remove(peers.find(peer => peer.peerNumber == peerNumber))
  }
}
