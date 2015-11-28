package chat.tox.antox.utils

import android.view.View

object ViewExtensions {
  implicit class RichView(val view: View) extends AnyVal {
    def getLocationOnScreen(): Location = {
      val rawLocation = Array.ofDim[Int](2)
      view.getLocationOnScreen(rawLocation)
      Location(rawLocation(0), rawLocation(1))
    }
  }
}
