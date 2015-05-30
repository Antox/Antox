package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.tox4j.core.callbacks.FriendTypingCallback

object AntoxOnTypingChangeCallback {

  private val TAG = "OnTypingChangeCallback"
}

class AntoxOnTypingChangeCallback(private var ctx: Context) extends FriendTypingCallback {

  override def friendTyping(friendNumber: Int, isTyping: Boolean): Unit = {
    ToxSingleton.typingMap.put(ToxSingleton.getAntoxFriend(friendNumber).get.getKey, isTyping)
    Reactive.typing.onNext(true)
  }
}
