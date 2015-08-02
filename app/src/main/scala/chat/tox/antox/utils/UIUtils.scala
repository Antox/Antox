package chat.tox.antox.utils

import chat.tox.antox.wrapper.ToxKey

object UIUtils {

  //Trims an ID so that it can be displayed to the user
  def trimId(id: ToxKey): String = {
    id.toString.substring(0, 7)
  }

  def removeNewlines(str: String): String = {
    str.replace("\n", "").replace("\r", "")
  }
}