package chat.tox.antox.toxdns

import scala.collection.JavaConversions._

object DnsName {
  def fromString(dnsName: String): DnsName = {
    val split = dnsName.split("@")
    DnsName(split(0), split.lift(1).map(_.replace("@", "")))
  }
}

final case class DnsName(user: String, domain: Option[String])
