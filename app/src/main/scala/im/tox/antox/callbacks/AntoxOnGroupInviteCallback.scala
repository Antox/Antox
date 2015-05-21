package im.tox.antox.callbacks

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import im.tox.antox.activities.MainActivity
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.Hex
import im.tox.antoxnightly.R

object AntoxOnGroupInviteCallback {

}

class AntoxOnGroupInviteCallback(private var ctx: Context) /* extends GroupInviteCallback */ {

  def groupInvite(friendNumber: Int, inviteData: Array[Byte]): Unit = {
    val db = new AntoxDB(this.ctx)
    val inviter = ToxSingleton.getAntoxFriend(friendNumber).get
    if (db.isContactBlocked(inviter.getKey)) return

    println("invite key is " + Hex.bytesToHexString(inviteData.slice(0, 32)))
    db.addGroupInvite(Hex.bytesToHexString(inviteData.slice(0, 32)), inviter.getName, inviteData)
    db.close()

    ToxSingleton.updateGroupInvites(ctx)
    Log.d("GroupInviteCallback", "")
    val preferences = PreferenceManager.getDefaultSharedPreferences(this.ctx)
    if (preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_group_invite", true)) {
      val vibratePattern = Array[Long](0, 500)
      if (!preferences.getBoolean("notifications_new_message_vibrate", true)) {
        vibratePattern(1) = 0
      }
      val mBuilder = new NotificationCompat.Builder(this.ctx)
        .setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(this.ctx.getString(R.string.group_invite))
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
