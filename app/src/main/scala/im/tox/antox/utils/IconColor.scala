package im.tox.antox.utils

import android.graphics.Color
import im.tox.jtoxcore.ToxUserStatus
import im.tox.antox.R
//remove if not needed
import scala.collection.JavaConversions._

object IconColor {

  def iconDrawable(isOnline: java.lang.Boolean, status: ToxUserStatus): Int = {
    val color = if (!isOnline) {
      R.drawable.circle_offline
    } else if (status == ToxUserStatus.TOX_USERSTATUS_NONE) {
      R.drawable.circle_online
    } else if (status == ToxUserStatus.TOX_USERSTATUS_AWAY) {
      R.drawable.circle_away
    } else if (status == ToxUserStatus.TOX_USERSTATUS_BUSY) {
      R.drawable.circle_busy
    } else {
      R.drawable.circle_offline
    }
    color
  }

}
