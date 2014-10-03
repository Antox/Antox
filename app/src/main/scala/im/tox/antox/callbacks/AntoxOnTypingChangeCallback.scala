package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.tox.Reactive
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.callbacks.OnTypingChangeCallback
import AntoxOnTypingChangeCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnTypingChangeCallback {

  private val TAG = "OnTypingChangeCallback"
}

class AntoxOnTypingChangeCallback(private var ctx: Context) extends OnTypingChangeCallback[AntoxFriend] {

  def execute(friend: AntoxFriend, typing: Boolean) {
    ToxSingleton.typingMap.put(friend.getId, typing)
    Reactive.typing.onNext(true)
  }
}
