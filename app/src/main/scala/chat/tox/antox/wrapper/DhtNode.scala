package chat.tox.antox.wrapper

import im.tox.core.network.Port
import im.tox.tox4j.core.data.ToxPublicKey

case class DhtNode(owner: String, ipv4: String, key: ToxPublicKey, port: Port)