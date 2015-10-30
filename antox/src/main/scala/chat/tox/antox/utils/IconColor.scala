package chat.tox.antox.utils

import chat.tox.antox.R
import im.tox.tox4j.core.enums.ToxUserStatus

object IconColor {

  def iconDrawable(isOnline: Boolean, status: ToxUserStatus): Int = {
    val color = if (!isOnline) {
      R.drawable.circle_offline
    } else if (status == ToxUserStatus.NONE) {
      R.drawable.circle_online
    } else if (status == ToxUserStatus.AWAY) {
      R.drawable.circle_away
    } else if (status == ToxUserStatus.BUSY) {
      R.drawable.circle_busy
    } else {
      R.drawable.circle_offline
    }
    color
  }

}
