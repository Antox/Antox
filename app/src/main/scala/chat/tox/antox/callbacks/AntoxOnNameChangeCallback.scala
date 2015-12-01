package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.utils.UiUtils
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.data.ToxNickname

class AntoxOnNameChangeCallback(private var ctx: Context) {
  def friendName(friendInfo: FriendInfo, nameBytes: ToxNickname)(state: Unit): Unit = {
    val name = UiUtils.removeNewlines(new String(nameBytes.value, "UTF-8"))

    val db = State.db
    db.updateContactName(friendInfo.key, name)
  }
}