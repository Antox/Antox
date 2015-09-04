package chat.tox.antox.av

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import chat.tox.antox.R
import chat.tox.antox.activities.{MainActivity, CallActivity}
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.Friend

class OngoingCallNotification(context: Context, friend: Friend, call: Call) {

  //ensure that this id is not the same as is used for messages
  val id: Long = friend.key.hashCode() + 1

  val builder = new NotificationCompat.Builder(context)
    .setSmallIcon(R.drawable.ic_actionbar)
    .setOngoing(true)
    .addAction(R.drawable.ic_call_end_white_36dp,
      context.getResources.getString(R.string.end_call),
      createPendingIntent(Constants.END_CALL, classOf[NotificationHandlerActivity], addParentStack = false))
    .setDefaults(Notification.DEFAULT_ALL)


  builder.setContentIntent(createPendingIntent(Constants.SWITCH_TO_CALL, classOf[CallActivity], addParentStack = true))

  def createPendingIntent(action: String, activity: Class[_], addParentStack: Boolean): PendingIntent = {
    val resultIntent = new Intent(context, activity)
    resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
    resultIntent.setAction(action)
    resultIntent.putExtra("key", friend.key)

    if (addParentStack) {
      val stackBuilder = TaskStackBuilder.create(context)
      stackBuilder.addParentStack(classOf[MainActivity])
      stackBuilder.addNextIntent(resultIntent)
      stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    } else {
      PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
  }

  def updateName(name: String): Unit = {
    builder.setContentText(name)
    builder.setSubText(name)
  }

  def show(): Unit = {
    ToxSingleton.mNotificationManager.notify(id.asInstanceOf[Int], builder.build())
  }

  def delete(): Unit = {
    ToxSingleton.mNotificationManager.cancel(id.asInstanceOf[Int])
  }
}
