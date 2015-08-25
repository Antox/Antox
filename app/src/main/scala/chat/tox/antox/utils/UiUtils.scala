package chat.tox.antox.utils

import java.util.Random

import android.graphics.Color
import chat.tox.antox.wrapper.ToxKey

object UiUtils {

  val trimedIdLength = 8
  //Trims an ID so that it can be displayed to the user
  def trimId(id: ToxKey): String = {
    id.toString.substring(0, trimedIdLength - 1)
  }

  def sanitizeAddress(address: String): String = {
  //remove start-of-file unicode char and spaces
    address.replaceAll("\uFEFF", "").replace(" ", "")
  }

  def removeNewlines(str: String): String = {
    str.replace("\n", "").replace("\r", "")
  }

  def generateColor(hash: Int): Int = {
    val goldenRatio = 0.618033988749895
    val hue: Double = (new Random(hash).nextFloat() + goldenRatio) % 1
    Color.HSVToColor(Array(hue.asInstanceOf[Float] * 360, 0.5f, 0.7f))
  }
}