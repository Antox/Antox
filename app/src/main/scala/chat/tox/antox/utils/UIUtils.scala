package chat.tox.antox.utils

import chat.tox.antox.wrapper.ToxKey

object UiUtils {

  val trimedIdLength = 8
  //Trims an ID so that it can be displayed to the user
  def trimId(id: ToxKey): String = {
    id.toString.substring(0, trimedIdLength - 1)
  }

  def sanitizeAddress(address: String): String = {
    address.replaceAll("\uFEFF", "").replace(" ", "") //remove start-of-file unicode char and spaces
  }

  def removeNewlines(str: String): String = {
    str.replace("\n", "").replace("\r", "")
  }
}