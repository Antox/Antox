package im.tox.antox.utils

class GroupPeer(val key: String,
                val peerNumber: Int,
                val name: String) {

  override def toString: String = name
}
