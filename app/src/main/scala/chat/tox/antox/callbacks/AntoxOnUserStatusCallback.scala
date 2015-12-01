package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.enums.ToxUserStatus

class AntoxOnUserStatusCallback(private var ctx: Context) {

  def friendStatus(friendInfo: FriendInfo, status: ToxUserStatus)(state: Unit): Unit = {
    val db = State.db
    db.updateContactStatus(friendInfo.key, status)
  }
}
