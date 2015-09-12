package chat.tox.antox.toxdns

import scala.collection.JavaConversions._

object DnsName {
  def fromString(dnsName: String): DnsName = {
    val split = dnsName.split("@")
    val domain = if (split.length == 1) ToxDNS.DEFAULT_TOXDNS_DOMAIN else split(1)
    DnsName(split(0), domain)
  }
}

final case class DnsName(username: String, domain: String){
  /**
   * @return username@domain
   */
  def getFullAddress: String = username + "@" + domain
}
