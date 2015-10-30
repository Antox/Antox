package chat.tox.antox.wrapper

import chat.tox.antox.utils.ToxPublicKey

class DhtNode(
  val owner: String,
  val ipv6: String,
  val ipv4: String,
  val key: ToxPublicKey,
  val port: Int) {
}
