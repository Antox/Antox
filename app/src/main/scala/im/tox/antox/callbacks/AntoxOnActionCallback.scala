package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.utils.{Constants, AntoxFriend}
import im.tox.jtoxcore.callbacks.OnActionCallback
import AntoxOnActionCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnActionCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnActionCallback(private var ctx: Context) extends OnActionCallback[AntoxFriend] {

  override def execute(friend: AntoxFriend, action: String): Unit = {
    AntoxOnMessageCallback.handleMessage(ctx, friend, formatAction(action, friend.name),
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
