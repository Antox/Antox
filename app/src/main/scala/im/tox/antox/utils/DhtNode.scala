package im.tox.antox.utils

//remove if not needed
import scala.collection.JavaConversions._

class DhtNode(
  val owner: String,
  val ipv6: String,
  val ipv4: String,
  val key: String,
  val port: Int) {
}
