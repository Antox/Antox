package im.tox.antox.utils

object UIUtils {

  //Trims an ID so that it can be displayed to the user
  def trimIDForDisplay(id: String): String = {
    id.substring(0, 7)
  }

  def removeNewlines(str: String): String = {
    str.replace("\n", "").replace("\r", "")
  }
}