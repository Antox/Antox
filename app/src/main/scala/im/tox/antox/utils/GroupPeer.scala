package im.tox.antox.utils

class GroupPeer(val id: String,
                val peerNumber: Int,
                val name: String,
                val ignored: Boolean) {

  override def toString: String = name
}
