package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.UiUtils
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.callbacks.FriendNameCallback

class AntoxOnNameChangeCallback(private var ctx: Context) {
  def friendName(friendInfo: FriendInfo, nameBytes: Array[Byte])(state: Unit): Unit = {
    val name = UiUtils.removeNewlines(new String(nameBytes, "UTF-8"))

    val db = State.db
    db.updateContactName(friendInfo.key, name)
  }
}