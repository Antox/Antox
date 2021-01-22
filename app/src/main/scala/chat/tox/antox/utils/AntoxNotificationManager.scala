package chat.tox.antox.utils

import android.app.{Notification, NotificationManager, PendingIntent}
import android.content.{Context, Intent, SharedPreferences}
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat.Builder
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.activities.MainActivity
import chat.tox.antox.av.MissedCallNotification
import chat.tox.antox.callbacks.AntoxOnSelfConnectionStatusCallback
import chat.tox.antox.data.{AntoxDB, CallEventKind, State}
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.TimestampUtils._
import chat.tox.antox.wrapper.{BitmapUtils, FriendKey, ToxKey, _}
import im.tox.tox4j.core.enums.{ToxConnection, ToxUserStatus}
import rx.lang.scala.Subscription
import rx.lang.scala.schedulers.AndroidMainThreadScheduler

object AntoxNotificationManager {

  var mNotificationManager: Option[NotificationManager] = None

  private val persistID = 12436
  private var persistBuilder: Option[NotificationCompat.Builder] = None
  private var statusSubscription: Option[Subscription] = None

  def checkPreference(preferences: SharedPreferences, notificationPreference: String): Boolean = {
    preferences.getBoolean("notifications_enable_notifications", true) && preferences.getBoolean(notificationPreference, true)
  }

  def generateNotificationId(key: ToxKey): Int = {
    key.hashCode()
  }

  def clearAllNotifications(): Unit = {
    mNotificationManager.foreach(_.cancelAll())
  }

  def addAvatarToNotification(builder: NotificationCompat.Builder, key: ToxKey): Unit = {
    AntoxLog.debug("Key class: " + key.getClass.getSimpleName)
    if (key.getClass == classOf[FriendKey]) {
      val friendInfo = State.db.getFriendInfo(key.asInstanceOf[FriendKey])
      if (friendInfo.avatar.isDefined) {
        val bitmapOptions = new BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeFile(friendInfo.avatar.get.getAbsolutePath, bitmapOptions)
        builder.setLargeIcon(BitmapUtils.getCircleBitmap(BitmapUtils.getCroppedBitmap(bitmap, recycle = false)))
      }
    }
  }

  def createMessageNotification(ctx: Context, intentClass: Class[_], contactInfo: ContactInfo, content: String, count: Int = 0): Unit = {
    AntoxLog.debug(s"Creating message notification, $contactInfo, $content")

    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    if (checkPreference(preferences, "notifications_new_message")) {
      val notificationBuilder = new Builder(ctx)
        .setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(contactInfo.getDisplayName)
        .setContentText(content)

      addAlerts(notificationBuilder, preferences)

      addAvatarToNotification(notificationBuilder, contactInfo.key)

      if (count > 0) {
        val countStr: String =
          if (count < 1000) s"$count" else "999+"

        notificationBuilder.setContentInfo(countStr)
      } else {
        notificationBuilder.setContentInfo("")
      }

      val resultPendingIntent: PendingIntent = createChatPendingIntent(ctx, Constants.SWITCH_TO_FRIEND, intentClass, contactInfo.key)

      notificationBuilder.setContentIntent(resultPendingIntent)

      val mNotificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
      mNotificationManager.notify(generateNotificationId(contactInfo.key), notificationBuilder.build())
    }
  }

  def createChatIntent(ctx: Context, action: String, intentClass: Class[_], key: ToxKey): Intent = {
    val resultIntent = new Intent(ctx, intentClass)
    resultIntent.setAction(action)
    resultIntent.putExtra("key", key.toString)
    resultIntent.putExtra("notification", true)

    resultIntent
  }

  def createChatPendingIntent(ctx: Context, action: String, intentClass: Class[_], key: ToxKey): PendingIntent = {
    val stackBuilder = TaskStackBuilder.create(ctx)
    stackBuilder.addParentStack(classOf[MainActivity])
    stackBuilder.addNextIntent(createChatIntent(ctx, action, intentClass, key))
    val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    resultPendingIntent
  }

  def clearMessageNotification(key: ToxKey): Unit = {
    mNotificationManager.foreach(_.cancel(generateNotificationId(key)))
  }

  var callMonitorSubscription: Option[Subscription] = None

  def startMonitoringCalls(context: Context, db: AntoxDB): Unit = {
    try {
      callMonitorSubscription = Some(db.messageListObservable(None)
        .map(_.filter(message => message.callEventKind == CallEventKind.Missed)) //only missed calls
        .map(_.filterNot(_.read)) // if the missed call has been seen it is not relevant
        .map(_.groupBy(_.key))
        .map(missedCallMap => missedCallMap.map(tuple => {
          val contactKey = tuple._1
          val missedCallEvents = tuple._2

          //a missed call is only relevant if a call has not been made to the same contact after the missed call
          def isRelevant(missedCallEvent: Message): Boolean = {
            val outgoingCallList = db.getMessageList(Some(contactKey)).filter(_.callEventKind == CallEventKind.Outgoing)
            if (outgoingCallList.nonEmpty) {
              val latestOutgoingTime = outgoingCallList.map(_.timestamp).max
              missedCallEvent.timestamp.after(latestOutgoingTime)
            } else true
          }

          val relevantMissedCallEvents = missedCallEvents.filter(isRelevant)

          (contactKey, relevantMissedCallEvents)
        }))
        .subscribe(missedCallMap => {
          for (key <- missedCallMap.keys) {
            val messageList = missedCallMap(key)
            val id = MissedCallNotification.id(key)

            val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

            // if there are no missed calls, hide the notification
            if (messageList.isEmpty) {
              mNotificationManager.cancel(id)
            } else {
              val notification = new MissedCallNotification(context, db.getFriendInfo(key.asInstanceOf[FriendKey]), messageList)
              mNotificationManager.notify(id, notification.build())
            }
          }
        }))
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  def stopMonitoringCalls(): Unit = {
    callMonitorSubscription.foreach(_.unsubscribe())
  }

  def createRequestNotification(key: ToxKey, contentText: Option[String], context: Context): Unit = {
    Log.d(AntoxNotificationManager.getClass.getSimpleName, s"Creating request notification")

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    if (checkPreference(preferences, "notifications_friend_request")) {
      val vibrateDuration = 500
      val vibratePattern = Array[Long](0, vibrateDuration)
      if (!preferences.getBoolean("notifications_new_message_vibrate", true)) {
        vibratePattern(1) = 0
      }

      val notificationBuilder = new NotificationCompat.Builder(context)
        .setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(context.getString(R.string.friend_request))
        .setVibrate(vibratePattern)
        .setAutoCancel(true)

      addAlerts(notificationBuilder, preferences)

      contentText.foreach(text => notificationBuilder.setContentText(text))

      val resultIntent = new Intent(context, classOf[MainActivity])
      resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
      val contentIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

      notificationBuilder.setContentIntent(contentIntent)

      mNotificationManager.foreach(_.notify(generateNotificationId(key), notificationBuilder.build()))
    }
  }

  def clearRequestNotification(key: ToxKey): Unit = {
    mNotificationManager.foreach(_.cancel(generateNotificationId(key)))
  }

  def createPersistentNotification(ctx: Context) {
    if (persistBuilder.isEmpty) {
      AntoxLog.debug("Creating persistent notification")

      val resultIntent = new Intent(ctx, classOf[MainActivity])
      val stackBuilder = TaskStackBuilder.create(ctx)
      stackBuilder.addParentStack(classOf[MainActivity])
      stackBuilder.addNextIntent(resultIntent)
      val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

      val builder = new NotificationCompat.Builder(ctx)
        .setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(ctx.getString(R.string.app_name))
        .setContentText(getStatus(ctx))
        .setContentIntent(resultPendingIntent)
        .setShowWhen(false)

      persistBuilder = Some(builder)

      val notification = builder.build()
      notification.flags = Notification.FLAG_ONGOING_EVENT
      mNotificationManager.foreach(_.notify(persistID, notification))

      statusSubscription = Some(State.userDb(ctx)
        .activeUserDetailsObservable()
        .combineLatestWith(AntoxOnSelfConnectionStatusCallback.connectionStatusSubject)((user, status) => (user, status))
        .observeOn(AndroidMainThreadScheduler())
        .subscribe(tuple => {
          updatePersistentNotification(ctx, tuple._2)
        }))
    }
  }

  def removePersistentNotification() {
    AntoxLog.debug("Removing persistent notification")

    statusSubscription.foreach(_.unsubscribe())
    mNotificationManager.foreach(_.cancel(persistID))
    persistBuilder = None
  }

  def updatePersistentNotification(ctx: Context, status: ToxConnection) {
    AntoxLog.debug("Updating persistent notification")

    persistBuilder.foreach(builder => {
      builder.setContentText(getStatus(ctx))
      val notification = builder.build()
      notification.flags = Notification.FLAG_ONGOING_EVENT
      mNotificationManager.foreach(_.notify(persistID, notification))
    })
  }

  private def getStatus(ctx: Context): String = {
    if (ToxSingleton.tox == null) {
      ctx.getString(R.string.status_offline)
    } else if (ToxSingleton.tox.getSelfConnectionStatus != ToxConnection.NONE) {
      ToxSingleton.tox.getStatus match {
        case ToxUserStatus.NONE =>
          ctx.getString(R.string.status_online)
        case ToxUserStatus.AWAY =>
          ctx.getString(R.string.status_away)
        case ToxUserStatus.BUSY =>
          ctx.getString(R.string.status_busy)
      }
    } else {
      ctx.getString(R.string.status_offline)
    }
  }

  def addAlerts(builder: NotificationCompat.Builder, preferences: SharedPreferences) {
    var defaults = 0
    if (checkPreference(preferences, "notifications_sound")) defaults |= Notification.DEFAULT_SOUND
    if (checkPreference(preferences, "notifications_vibrate")) defaults |= Notification.DEFAULT_VIBRATE else builder.setVibrate(Array(0L))
    if (checkPreference(preferences, "notifications_light")) defaults |= Notification.DEFAULT_LIGHTS
    if (defaults != 0) builder.setDefaults(defaults)
  }

}
