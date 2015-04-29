package im.tox.antox.utils

import im.tox.antoxnightly.R
import im.tox.tox4j.core.enums.ToxStatus

object IconColor {

  def iconDrawable(isOnline: java.lang.Boolean, status: ToxStatus): Int = {
    val color = if (!isOnline) {
      R.drawable.circle_offline
    } else if (status == ToxStatus.NONE) {
      R.drawable.circle_online
    } else if (status == ToxStatus.AWAY) {
      R.drawable.circle_away
    } else if (status == ToxStatus.BUSY) {
      R.drawable.circle_busy
    } else {
      R.drawable.circle_offline
    }
    color
  }

}
