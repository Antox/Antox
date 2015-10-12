package chat.tox.antox.utils

import android.app.{NotificationManager, PendingIntent, Notification}
import android.content.{SharedPreferences, Intent, Context}
import android.graphics._
import android.preference.PreferenceManager
import android.support.v4.app.{TaskStackBuilder, NotificationCompat}
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.activities.MainActivity
import chat.tox.antox.callbacks.AntoxOnSelfConnectionStatusCallback
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.wrapper.{BitmapUtils, FriendKey, UserInfo, ToxKey}
import im.tox.tox4j.core.enums.{ToxConnection, ToxUserStatus}
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.Subscription

object AntoxNotificationManager {

  var mNotificationManager: Option[NotificationManager] = None

  private val persistID = 12436
  private var persistBuilder: NotificationCompat.Builder = _
  private var statusSubscription: Subscription = _
  var persistOn = false


  def checkPreference(preferences: SharedPreferences, notificationPreference: String): Boolean = {
    preferences.getBoolean("notifications_enable_notifications", true) && preferences.getBoolean(notificationPreference, true)
  }


  def generateNotificationId(key: ToxKey): Int = key.hashCode()

  def createMessageNotification(ctx: Context, intentClass: Class[_], key: ToxKey, name: String, content: String): Unit = {

    AntoxLog.debug( s"Creating message notification, $name, $content")

    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)


    if (checkPreference(preferences, "notifications_new_message")) {
      val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(name)
        .setContentText(content)

      if(checkPreference(preferences, "notifications_sound") && checkPreference(preferences, "notifications_vibrate")) {
        mBuilder.setDefaults(Notification.DEFAULT_ALL)
      }
      else if(checkPreference(preferences, "notifications_sound"))mBuilder.setDefaults(Notification.DEFAULT_SOUND)
      else if(checkPreference(preferences, "notifications_vibrate"))mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)

      if(checkPreference(preferences, "notifications_light")){
        mBuilder.setLights(Color.GREEN, 500, 500)
      }

      AntoxLog.debug("Key class: " + key.getClass.getSimpleName)
      if(key.getClass == classOf[FriendKey]){
        val friendInfo = State.db.getFriendInfo(key.asInstanceOf[FriendKey])
        if(friendInfo.avatar.isDefined){
          val bmOptions = new BitmapFactory.Options()
          val bitmap = BitmapFactory.decodeFile(friendInfo.avatar.get.getAbsolutePath,bmOptions)
          mBuilder.setLargeIcon(BitmapUtils.getCroppedBitmap(bitmap))
        }
      }

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
    Log.d(AntoxNotificationManager.getClass.getSimpleName, s"Creating request notification")

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    if (checkPreference(preferences, "notifications_friend_request")) {
      val vibrateDuration = 500
      val vibratePattern = Array[Long](0, vibrateDuration)
      if (!preferences.getBoolean("notifications_new_message_vibrate", true)) {
        vibratePattern(1) = 0
      }

      val mBuilder = new NotificationCompat.Builder(context)
        .setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(context.getString(R.string.friend_request))
        .setVibrate(vibratePattern)
        .setAutoCancel(true)

      if(checkPreference(preferences, "notifications_sound") && checkPreference(preferences, "notifications_vibrate")) {
        mBuilder.setDefaults(Notification.DEFAULT_ALL)
      }
      else if(checkPreference(preferences, "notifications_sound"))mBuilder.setDefaults(Notification.DEFAULT_SOUND)
      else if(checkPreference(preferences, "notifications_vibrate"))mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)

      if(checkPreference(preferences, "notifications_light")){
        mBuilder.setLights(Color.GREEN, 500, 500)
      }

      val notif = mBuilder.build()

      contentText.foreach(text => mBuilder.setContentText(text))

      val targetIntent = new Intent(context, classOf[MainActivity])
      val contentIntent = PendingIntent.getActivity(context, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT)

      mBuilder.setContentIntent(contentIntent)
      mNotificationManager.foreach(_.notify(0, notif))
    }
  }

  def createPersistentNotification(ctx: Context): Unit ={

    if(persistOn) return

    persistOn = true

    AntoxLog.debug("Creating persistent notification")

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

    val resultIntent = new Intent(ctx, classOf[MainActivity])
    val stackBuilder = TaskStackBuilder.create(ctx)
    stackBuilder.addParentStack(classOf[MainActivity])
    stackBuilder.addNextIntent(resultIntent)
    val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

    persistBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
      .setContentTitle(ctx.getString(R.string.app_name))
      .setContentText(status)
      .setContentIntent(resultPendingIntent)
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

    AntoxLog.debug( "Removing persistent notification")

    if(statusSubscription != null) statusSubscription.unsubscribe()
    mNotificationManager.foreach(_.cancel(persistID))
  }

  def updatePersistentNotification(ctx: Context, userInfo: UserInfo, toxConnection: ToxConnection): Unit ={

    AntoxLog.debug("Updating persistent notification")

    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    if(persistOn){

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
