package chat.tox.antox.wrapper

import chat.tox.antox.utils.PeerKey

class GroupPeer(var key: PeerKey,
                var name: String,
                var ignored: Boolean) {

  override def toString: String = name
}
