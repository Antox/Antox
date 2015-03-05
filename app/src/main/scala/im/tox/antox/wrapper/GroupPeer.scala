package im.tox.antox.wrapper

class GroupPeer(val peerNumber: Int,
                var name: String,
                var ignored: Boolean) {

  def this(peerNumber: Int) {
    this(peerNumber, "", false)
  }

  override def toString: String = name
}
