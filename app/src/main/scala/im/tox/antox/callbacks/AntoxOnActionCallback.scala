package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{AntoxFriend, Constants}
import im.tox.tox4j.core.callbacks.FriendActionCallback

object AntoxOnActionCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnActionCallback(private var ctx: Context) extends FriendActionCallback {

  override def friendAction(friendNumber: Int, timeDelta: Int, message: Array[Byte]): Unit = {
    AntoxOnMessageCallback.handleMessage(ctx, friendNumber,
      ToxSingleton.getAntoxFriend(friendNumber).get.getKey(),
      new String(message, "UTF-8"),
      Constants.MESSAGE_TYPE_ACTION)
  }
}
