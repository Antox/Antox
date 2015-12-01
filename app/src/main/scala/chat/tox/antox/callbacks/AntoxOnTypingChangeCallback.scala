package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.wrapper.FriendInfo

class AntoxOnTypingChangeCallback(private var ctx: Context) {

  def friendTyping(friendInfo: FriendInfo, isTyping: Boolean)(state: Unit): Unit = {
    ToxSingleton.typingMap.put(friendInfo.key, isTyping)
    State.typing.onNext(true)
  }
}
