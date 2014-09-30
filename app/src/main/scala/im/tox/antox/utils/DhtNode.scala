package im.tox.antox.utils

import java.util.ArrayList
//remove if not needed
import scala.collection.JavaConversions._

object DhtNode {

  var ipv4: ArrayList[String] = new ArrayList[String]()

  var ipv6: ArrayList[String] = new ArrayList[String]()

  var port: ArrayList[String] = new ArrayList[String]()

  var key: ArrayList[String] = new ArrayList[String]()

  var owner: ArrayList[String] = new ArrayList[String]()

  var counter: Int = 0

  var connected: Boolean = false

  var sorted: Boolean = false
}
