package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.{Reactive, ToxSingleton}
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.callbacks.FriendTypingCallback

class AntoxOnTypingChangeCallback(private var ctx: Context) {

  def friendTyping(friendInfo: FriendInfo, isTyping: Boolean)(state: Unit): Unit = {
    ToxSingleton.typingMap.put(friendInfo.key, isTyping)
    Reactive.typing.onNext(true)
  }
}
