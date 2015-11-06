package chat.tox.antox.toxme

object ToxMeError extends Enumeration {
  type ToxMeError = Value
  val OK = Value("0")
  val METHOD_UNSUPPORTED = Value("-1")
  val NOTSECURE = Value("-2")
  val BAD_PAYLOAD = Value("-3")
  val NAME_TAKEN = Value("-25")
  val DUPE_ID = Value("-26")
  val UNKNOWN_NAME = Value("-30")
  val INVALID_ID = Value("-31")
  val LOOKUP_FAILED = Value("-41")
  val NO_USER = Value("-42")
  val LOOKUP_INTERNAL = Value("-43")
  val RATE_LIMIT = Value("-4")
  val KALIUM_LINK_ERROR = Value("KALIUM")
  val INVALID_DOMAIN = Value("INVALID_DOMAIN")
  val INTERNAL = Value("INTERNAL")
  val UNKNOWN = Value("")
  val JSON_ERROR = Value("JSON")
  val ENCODING_ERROR = Value("ENCODING")
  val CONNECTION_ERROR = Value("CONNECTION")

  def valueOf(name: String): Option[ToxMeError.Value] = values.find(_.toString == name)

  def exception(exception: Exception): ToxMeError = {
    Value(exception.getClass.getSimpleName + ": " + exception.getMessage)
  }

  def getDebugDescription(toxmeError: ToxMeError):String = {
    toxmeError match {
      case OK => "OK"
      case METHOD_UNSUPPORTED => "Client didn't POST to /api"
      case NOTSECURE => "Client is not using a secure connection"
      case BAD_PAYLOAD => "Bad encrypted payload (not encrypted with public key)"
      case NAME_TAKEN => "Name is taken"
      case DUPE_ID => "The public key given is bound to a name already"
      case UNKNOWN_NAME => "Name not found"
      case INVALID_ID => "Sent invalid data in place of an ID"
      case LOOKUP_FAILED => "Lookup failed because of an error on the other domain's side."
      case NO_USER => "Lookup failed because that user doesn't exist on the domain"
      case LOOKUP_INTERNAL => "Lookup failed because of a server error"
      case RATE_LIMIT => "Client is publishing IDs too fast"
      case KALIUM_LINK_ERROR => "Kalium link error"
      case INVALID_DOMAIN => "Invalid ToxMe domain"
      case INTERNAL => "Internal error"
      case UNKNOWN => "Unknown error code"
      case JSON_ERROR => "Error constructing JSON"
      case ENCODING_ERROR => "Encoding error"
      case _ => toxmeError.toString
    }
  }
}
