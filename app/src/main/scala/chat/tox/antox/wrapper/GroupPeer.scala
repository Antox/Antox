package chat.tox.antox.wrapper

class GroupPeer(var name: String,
                var ignored: Boolean) {

  override def toString: String = name
}
