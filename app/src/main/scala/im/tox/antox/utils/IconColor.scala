package im.tox.antox.utils

import android.graphics.Color
import im.tox.jtoxcore.ToxUserStatus
import im.tox.antox.R
//remove if not needed
import scala.collection.JavaConversions._

object IconColor {

  def iconColorAsString(isOnline: java.lang.Boolean, status: ToxUserStatus): String = {
    var color: String = null
    color = if (!isOnline) "#B0B0B0" else if (status == ToxUserStatus.TOX_USERSTATUS_NONE) "#5ec245" else if (status == ToxUserStatus.TOX_USERSTATUS_AWAY) "#E5C885" else if (status == ToxUserStatus.TOX_USERSTATUS_BUSY) "#CF4D58" else "#FFFFFF"
    color
  }

  def iconDrawable(isOnline: java.lang.Boolean, status: ToxUserStatus): Int = {
    val color = if (!isOnline) {
      R.drawable.circle_grey_dark
    } else if (status == ToxUserStatus.TOX_USERSTATUS_NONE) {
      R.drawable.circle_green
    } else if (status == ToxUserStatus.TOX_USERSTATUS_AWAY) {
      R.drawable.circle_green
    } else if (status == ToxUserStatus.TOX_USERSTATUS_BUSY) {
      R.drawable.circle_green
    } else {
      R.drawable.circle_grey_dark
    }
    color
  }

  def iconColorAsColor(isOnline: java.lang.Boolean, status: ToxUserStatus): Int = {
    Color.parseColor(iconColorAsString(isOnline, status))
  }
}
