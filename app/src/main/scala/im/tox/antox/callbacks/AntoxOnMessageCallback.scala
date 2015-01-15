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
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{AntoxFriend, Constants}
import im.tox.tox4j.core.callbacks.FriendMessageCallback

//remove if not needed

object AntoxOnMessageCallback {

  val TAG = "im.tox.antox.callbacks.AntoxOnMessageCallback"

 def handleMessage(ctx: Context, friendNumber: Int, friendId: String, message: String, messageType: Int): Unit ={
    val db = new AntoxDB(ctx)
    val friendAddress = ToxSingleton.addressFromClientId(friendId)

     Log.d(TAG, "friend id: " + friendAddress + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    if (!db.isFriendBlocked(friendAddress)) {
      if (!(State.chatActive && State.activeKey.map(_ == friendAddress).getOrElse(false))) {
        db.addMessage(-1, friendAddress, message, true, false, true, messageType)
      } else {
        db.addMessage(-1, friendAddress, message, true, true, true, messageType)
      }
    }
    db.close()
    ToxSingleton.updateMessages(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    if (preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_new_message", true)) {
      if (!(State.chatActive && State.activeKey.map(_ == friendAddress).getOrElse(false))) {
        val mName = ToxSingleton.getAntoxFriend(friendAddress).map(_.getName)
        mName.foreach(name => {
          val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
            .setContentTitle(name)
            .setContentText(message)
            .setDefaults(Notification.DEFAULT_ALL)
          val resultIntent = new Intent(ctx, classOf[MainActivity])
          resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
          resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
          resultIntent.putExtra("key", friendAddress)
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
}

class AntoxOnMessageCallback(private var ctx: Context) extends FriendMessageCallback {

  override def friendMessage(friendNumber: Int, timeDelta: Int, message: Array[Byte]): Unit = {
    handleMessage(ctx, friendNumber, ToxSingleton.getIdFromFriendNumber(friendNumber), new String(message, "UTF-8"), Constants.MESSAGE_TYPE_FRIEND)
  }
}
