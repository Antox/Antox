package chat.tox.antox.toxme

object ToxMeName {
  def fromString(toxMeName: String, useToxMe: Boolean): ToxMeName = {
    val split = toxMeName.split("@")
    val domain =
      if (useToxMe) {
        val domain = if (split.length == 1) ToxMe.DEFAULT_TOXME_DOMAIN else split(1)
        Some(domain)
      } else None

    ToxMeName(split(0), domain)
  }
}

final case class ToxMeName(username: String, domain: Option[String]){
  /**
   * @return username@domain
   */
  def getFullAddress: String = username + "@" + domain
}
