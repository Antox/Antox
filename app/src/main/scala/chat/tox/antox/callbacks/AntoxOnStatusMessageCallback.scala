package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.UiUtils
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.callbacks.FriendStatusMessageCallback

class AntoxOnStatusMessageCallback(private var ctx: Context) {

  def friendStatusMessage(friendInfo: FriendInfo, messageBytes: Array[Byte])(state: Unit): Unit = {
    val statusMessage = UiUtils.removeNewlines(new String(messageBytes, "UTF-8"))

    val db = State.db
    db.updateContactStatusMessage(friendInfo.key, statusMessage)
  }
}
