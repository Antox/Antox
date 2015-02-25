package im.tox.antox.callbacks

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import android.util.Log
import im.tox.antoxnightly.R
import im.tox.antox.activities.MainActivity
import im.tox.antox.callbacks.AntoxOnMessageCallback._
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{AntoxFriend, Constants}
import im.tox.tox4j.core.callbacks.FriendMessageCallback

object AntoxOnMessageCallback {

  val TAG = "im.tox.antox.callbacks.AntoxOnMessageCallback"

  def handleMessage(ctx: Context, friendNumber: Int, friendKey: String, rawMessage: String, messageType: Int): Unit = {
    val db = new AntoxDB(ctx)
    val message = if (messageType == Constants.MESSAGE_TYPE_ACTION) {
      val friendDetails = db.getFriendDetails(friendKey)
      formatAction(rawMessage, if (friendDetails(1) == "") friendDetails(0) else friendDetails(1))
    } else {
      rawMessage
    }

    Log.d(TAG, "friend id: " + friendKey + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    if (!db.isFriendBlocked(friendKey)) {
      val chatActive = (State.chatActive && State.activeKey.contains(friendKey))
        db.addMessage(-1, friendKey, message, has_been_received = true,
                      has_been_read = chatActive, successfully_sent = true, messageType)
    }
    db.close()
    ToxSingleton.updateMessages(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    if (preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_new_message", true)) {
      if (!(State.chatActive && State.activeKey.contains(friendKey))) {
        val mName = ToxSingleton.getAntoxFriend(friendKey).map(_.getName)
        mName.foreach(name => {
          val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
            .setContentTitle(name)
            .setContentText(message)
            .setDefaults(Notification.DEFAULT_ALL)
          val resultIntent = new Intent(ctx, classOf[MainActivity])
          resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
          resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
          resultIntent.putExtra("key", friendKey)
          resultIntent.putExtra("name", name)
          val stackBuilder = TaskStackBuilder.create(ctx)
          stackBuilder.addParentStack(classOf[MainActivity])
          stackBuilder.addNextIntent(resultIntent)
          val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
          mBuilder.setContentIntent(resultPendingIntent)
          ToxSingleton.mNotificationManager.notify(friendNumber, mBuilder.build())
        })
      }
    }
  }

  def formatAction(action: String, friendName: String): String = {
    var formattedAction = ""
    if (!action.startsWith(friendName)) {
      formattedAction = friendName + " " + action
    }

    formattedAction
  }
}

class AntoxOnMessageCallback(private var ctx: Context) extends FriendMessageCallback {

  override def friendMessage(friendNumber: Int, timeDelta: Int, message: Array[Byte]): Unit = {
    handleMessage(ctx, friendNumber, ToxSingleton.getAntoxFriend(friendNumber).get.getKey, new String(message, "UTF-8"), Constants.MESSAGE_TYPE_FRIEND)
  }
}
