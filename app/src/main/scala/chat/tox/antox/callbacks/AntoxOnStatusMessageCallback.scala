package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.data.ToxStatusMessage

class AntoxOnStatusMessageCallback(private var ctx: Context) {

  def friendStatusMessage(friendInfo: FriendInfo, message: ToxStatusMessage)(state: Unit): Unit = {

    val db = State.db
    db.updateContactStatusMessage(friendInfo.key, message)
  }
}
