package chat.tox.antox.callbacks

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.activities.MainActivity
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.wrapper.ToxKey
import im.tox.tox4j.core.callbacks.FriendRequestCallback

object AntoxOnFriendRequestCallback {

  private val TAG = "chat.tox.antox.TAG"

  val FRIEND_KEY = "chat.tox.antox.FRIEND_KEY"

  val FRIEND_MESSAGE = "chat.tox.antox.FRIEND_MESSAGE"
}

class AntoxOnFriendRequestCallback(private var ctx: Context) extends FriendRequestCallback[Unit] {

  override def friendRequest(keyBytes: Array[Byte], timeDelta: Int, message: Array[Byte])(state: Unit): Unit = {
    val db = State.db
    val key = new ToxKey(keyBytes)
    if (!db.isContactBlocked(key)){
      db.addFriendRequest(key, new String(message, "UTF-8"))
    }

    Log.d("FriendRequestCallback", "")
    val preferences = PreferenceManager.getDefaultSharedPreferences(this.ctx)
    if (preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_friend_request", true)) {
      val vibratePattern = Array[Long](0, 500)
      if (!preferences.getBoolean("notifications_new_message_vibrate", true)) {
        vibratePattern(1) = 0
      }
      val mBuilder = new NotificationCompat.Builder(this.ctx)
        .setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(this.ctx.getString(R.string.friend_request))
        .setContentText(new String(message, "UTF-8"))
        .setVibrate(vibratePattern)
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
      val targetIntent = new Intent(this.ctx, classOf[MainActivity])
      val contentIntent = PendingIntent.getActivity(this.ctx, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT)
      mBuilder.setContentIntent(contentIntent)
      ToxSingleton.mNotificationManager.notify(0, mBuilder.build())
    }
  }
}
