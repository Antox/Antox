package chat.tox.antox.theme

import android.graphics.Color
import chat.tox.antox.R

object ThemeManager {

  var primaryColor: Int = R.color.brand_primary
  var darkerPrimaryColor: Int = R.color.brand_primary_dark

  def darkenColor(color: Int): Int = {
    val hsv: Array[Float] = new Array[Float](3)
    Color.colorToHSV(color, hsv)
    hsv(2) *= 0.9f
    Color.HSVToColor(hsv)
  }
}
