package chat.tox.antox.utils

import android.app.{NotificationManager, PendingIntent, Notification}
import android.content.{SharedPreferences, Intent, Context}
import android.preference.PreferenceManager
import android.support.v4.app.{TaskStackBuilder, NotificationCompat}
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.activities.MainActivity
import chat.tox.antox.callbacks.AntoxOnSelfConnectionStatusCallback
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.wrapper.{UserInfo, ToxKey}
import im.tox.tox4j.core.enums.{ToxConnection, ToxUserStatus}
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.Subscription
import scala.util.Random

object AntoxNotificationManager {

  var mNotificationManager: Option[NotificationManager] = None

  private val persistID = 12436
  private var persistBuilder: NotificationCompat.Builder = _
  private var statusSubscription: Subscription = _
  var persistOn = false



  def shouldNotify(preferences: SharedPreferences, notificationPreference: String): Boolean = {
    preferences.getBoolean("notifications_enable_notifications", true) && preferences.getBoolean(notificationPreference, true)
  }

  def generateNotificationId(key: ToxKey): Int = key.hashCode()

  def createMessageNotification(ctx: Context, intentClass: Class[_], key: ToxKey, name: String, content: String): Unit = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    if (shouldNotify(preferences, "notifications_new_message")) {
      val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(name)
        .setContentText(content)
        .setDefaults(Notification.DEFAULT_ALL)

      val resultIntent = new Intent(ctx, intentClass)
      resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
      resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
      resultIntent.putExtra("key", key.toString)
      resultIntent.putExtra("name", name)

      val stackBuilder = TaskStackBuilder.create(ctx)
      stackBuilder.addParentStack(classOf[MainActivity])
      stackBuilder.addNextIntent(resultIntent)
      val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

      mBuilder.setContentIntent(resultPendingIntent)
      mNotificationManager.foreach(_.notify(generateNotificationId(key), mBuilder.build()))
    }
  }

  def clearMessageNotification(key: ToxKey) {
    mNotificationManager.foreach(_.cancel(generateNotificationId(key)))
  }

  def createRequestNotification(contentText: Option[String], context: Context): Unit = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    if (shouldNotify(preferences, "notifications_friend_request")) {
      val vibrateDuration = 500
      val vibratePattern = Array[Long](0, vibrateDuration)
      if (!preferences.getBoolean("notifications_new_message_vibrate", true)) {
        vibratePattern(1) = 0
      }

      val mBuilder = new NotificationCompat.Builder(context)
        .setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(context.getString(R.string.friend_request))
        .setVibrate(vibratePattern)
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)

      contentText.foreach(text => mBuilder.setContentText(text))

      val targetIntent = new Intent(context, classOf[MainActivity])
      val contentIntent = PendingIntent.getActivity(context, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT)

      mBuilder.setContentIntent(contentIntent)
      mNotificationManager.foreach(_.notify(0, mBuilder.build()))
    }
  }

  def createPersistentNotification(ctx: Context): Unit ={

    if(persistOn) return

    persistOn = true

    Log.d(AntoxNotificationManager.getClass.getSimpleName, "Creating persistent notification")

    val status = if(!ToxSingleton.isToxConnected(PreferenceManager.getDefaultSharedPreferences(ctx), ctx)){
      ctx.getString(R.string.status_offline)
    }
    else if(ToxSingleton.tox == null) ctx.getString(R.string.status_offline)
    else ToxSingleton.tox.getStatus match {
      case ToxUserStatus.NONE =>
        ctx.getString(R.string.status_online)
      case ToxUserStatus.AWAY =>
        ctx.getString(R.string.status_away)
      case ToxUserStatus.BUSY =>
        ctx.getString(R.string.status_busy)
    }

    persistBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
      .setContentTitle(ctx.getString(R.string.app_name))
      .setContentText(status)
    val notif = persistBuilder.build()
    notif.flags = Notification.FLAG_ONGOING_EVENT
    mNotificationManager.foreach(_.notify(persistID,notif))

    statusSubscription = State.userDb(ctx)
      .activeUserDetailsObservable()
      .combineLatestWith(AntoxOnSelfConnectionStatusCallback.connectionStatusSubject)((user, status) => (user, status))
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(tuple => {
      updatePersistentNotification(ctx, tuple._1, tuple._2)
    })
  }

  def removePersistentNotification(): Unit ={

    persistOn = false

    Log.d(AntoxNotificationManager.getClass.getSimpleName, "Removing persistent notification")

    if(statusSubscription != null) statusSubscription.unsubscribe()
    mNotificationManager.foreach(_.cancel(persistID))
  }

  def updatePersistentNotification(ctx: Context, userInfo: UserInfo, toxConnection: ToxConnection): Unit ={

    Log.d(AntoxNotificationManager.getClass.getSimpleName, "Updating persistent notification")

    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    if(preferences.getBoolean("notifications_persistent",false)){

      val status = if(toxConnection == ToxConnection.NONE ||
        ToxSingleton.tox == null) ctx.getString(R.string.status_offline)
      else ToxSingleton.tox.getStatus match {
        case ToxUserStatus.NONE =>
          ctx.getString(R.string.status_online)
        case ToxUserStatus.AWAY =>
          ctx.getString(R.string.status_away)
        case ToxUserStatus.BUSY =>
          ctx.getString(R.string.status_busy)
      }

      persistBuilder.setContentText(status)
      val notif = persistBuilder.build()
      notif.flags = Notification.FLAG_ONGOING_EVENT
      mNotificationManager.foreach(_.notify(persistID,notif))
    }
  }
}
