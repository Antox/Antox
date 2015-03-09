package im.tox.antox.wrapper

import im.tox.antox.tox.ToxSingleton

class Group(val id: String,
            val groupNumber: Int,
            var name: String,
            var alias: String,
            var topic: String,
            val peers: PeerList) {

  def leave(partMessage: String): Unit = {
    ToxSingleton.tox.deleteGroup(groupNumber, partMessage)
  }

  override def toString: String = name
}
