package im.tox.antox.callbacks

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import android.util.Log
import im.tox.antox.R
import im.tox.antox.activities.MainActivity
import im.tox.antox.callbacks.AntoxOnMessageCallback._
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.{MessageHelper, ToxSingleton}
import im.tox.antox.utils.{Hex, Constants}
import im.tox.antox.wrapper.MessageType
import im.tox.tox4j.core.callbacks.FriendMessageCallback
import im.tox.tox4j.core.enums.ToxMessageType

object AntoxOnMessageCallback {

  val TAG = "im.tox.antox.callbacks.AntoxOnMessageCallback"
}

class AntoxOnMessageCallback(private var ctx: Context) extends FriendMessageCallback {

  override def friendMessage(friendNumber: Int, messageType: ToxMessageType, timeDelta: Int, message: Array[Byte]): Unit = {
    MessageHelper.handleMessage(ctx, friendNumber,
      ToxSingleton.getAntoxFriend(friendNumber).get.getKey,
      new String(message, "UTF-8"), MessageType.fromToxMessageType(messageType))
  }
}
