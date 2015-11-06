package chat.tox.antox.utils

object StringExtensions {
  implicit class RichString(val string: String) extends AnyVal {
    def toOption: Option[String] = if (string.isEmpty) None else Some(string)
  }
}
