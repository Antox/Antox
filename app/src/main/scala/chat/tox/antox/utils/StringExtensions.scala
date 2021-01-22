package chat.tox.antox.utils

object StringExtensions {

  implicit class RichString(val string: String) extends AnyVal {
    /**
      * Convenience method to convert [[String]] to [[scala.Option]][String]
      *
      * @return None if the string `.isEmpty`, otherwise `Some(string)`
      */
    def toOption: Option[String] = if (string.isEmpty) None else Some(string)
  }

}
