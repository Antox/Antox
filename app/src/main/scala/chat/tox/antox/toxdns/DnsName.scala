package chat.tox.antox.toxdns

import scala.collection.JavaConversions._

object DnsName {
  def fromString(dnsName: String, useDns: Boolean): DnsName = {
    val split = dnsName.split("@")
    val domain = if (useDns) {
      val dom = if (split.length == 1) ToxDNS.DEFAULT_TOXDNS_DOMAIN else split(1)
      Some(dom)
    }
    else None
    DnsName(split(0), domain)
  }
}

final case class DnsName(username: String, domain: Option[String]){
  /**
   * @return username@domain
   */
  def getFullAddress: String = username + "@" + domain
}
