package chat.tox.antox.wrapper

import im.tox.core.network.Port
import im.tox.tox4j.core.data.ToxPublicKey

class DhtNode(
  val owner: String,
  val ipv6: String,
  val ipv4: String,
  val key: ToxPublicKey,
  val port: Port) {
}
