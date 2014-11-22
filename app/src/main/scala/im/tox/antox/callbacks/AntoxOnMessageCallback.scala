package im.tox.antox.callbacks

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import im.tox.antox.R
import im.tox.antox.activities.MainActivity
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.Constants
import im.tox.antox.data.State
import im.tox.jtoxcore.callbacks.OnMessageCallback
import AntoxOnMessageCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnMessageCallback {

  val TAG = "im.tox.antox.callbacks.AntoxOnMessageCallback"

 def handleMessage(ctx: Context, friend: AntoxFriend, message: String, messageType: Int): Unit ={
    val db = new AntoxDB(ctx)
    Log.d(TAG, "friend id: " + friend.getId + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    if (!db.isFriendBlocked(friend.getId)) {
      if (!(State.chatActive && State.activeKey.map(_ == friend.getId).getOrElse(false))) {
        db.addMessage(-1, friend.getId, message, true, false, true, messageType)
      } else {
        db.addMessage(-1, friend.getId, message, true, true, true, messageType)
      }
    }
    db.close()
    ToxSingleton.updateMessages(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    if (preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_new_message", true)) {
      if (!(State.chatActive && State.activeKey.map(_ == friend.getId).getOrElse(false))) {
        val mName = ToxSingleton.getAntoxFriend(friend.getId).map(_.getName)
        mName.foreach(name => {
          val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
            .setContentTitle(name)
            .setContentText(message)
            .setDefaults(Notification.DEFAULT_ALL)
          val resultIntent = new Intent(ctx, classOf[MainActivity])
          resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
          resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
          resultIntent.putExtra("key", friend.getId)
          resultIntent.putExtra("name", name)
          val stackBuilder = TaskStackBuilder.create(ctx)
          stackBuilder.addParentStack(classOf[MainActivity])
          stackBuilder.addNextIntent(resultIntent)
          val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
          mBuilder.setContentIntent(resultPendingIntent)
          ToxSingleton.mNotificationManager.notify(friend.getFriendnumber, mBuilder.build())
        })
      }
    }
  }
}

class AntoxOnMessageCallback(private var ctx: Context) extends OnMessageCallback[AntoxFriend] {

  override def execute(friend: AntoxFriend, message: String) {
    handleMessage(ctx, friend, message, Constants.MESSAGE_TYPE_FRIEND)
  }
}
