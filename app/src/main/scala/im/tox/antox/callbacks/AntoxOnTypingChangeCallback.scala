package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.antox.utils.{Hex, AntoxFriend}
import im.tox.tox4j.core.callbacks.FriendTypingCallback

//remove if not needed

object AntoxOnTypingChangeCallback {

  private val TAG = "OnTypingChangeCallback"
}

class AntoxOnTypingChangeCallback(private var ctx: Context) extends FriendTypingCallback {

  override def friendTyping(friendNumber: Int, isTyping: Boolean): Unit = {
    ToxSingleton.typingMap.put(ToxSingleton.getIdFromFriendNumber(friendNumber), isTyping)
    Reactive.typing.onNext(true)
  }
}
