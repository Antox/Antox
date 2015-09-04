package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.tox4j.core.callbacks.FriendTypingCallback

object AntoxOnTypingChangeCallback {

  private val TAG = "OnTypingChangeCallback"
}

class AntoxOnTypingChangeCallback(private var ctx: Context) extends FriendTypingCallback[Unit] {

  override def friendTyping(friendNumber: Int, isTyping: Boolean)(state: Unit): Unit = {
    ToxSingleton.typingMap.put(ToxSingleton.getAntoxFriend(friendNumber).get.getKey, isTyping)
    Reactive.typing.onNext(true)
  }
}
