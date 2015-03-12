package im.tox.antox.utils

object IDUtils {

  //Trims an ID so that it can be displayed to the user
  def trimForUI(id: String): String = {
    id.substring(0, 7)
  }
}