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
      ToxSingleton.getIdFromFriendNumber(friendNumber),
      formatAction(new String(message, "UTF-8"), ToxSingleton.getNameFromFriendNumber(friendNumber)),
      Constants.MESSAGE_TYPE_ACTION)
  }

  def formatAction(action: String, friendName: String): String = {
    var formattedAction = ""
    if (!action.startsWith(friendName)) {
      formattedAction = friendName + " " + action
    }

    return formattedAction
  }
}
