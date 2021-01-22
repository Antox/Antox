package chat.tox.antox.av

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import chat.tox.antox.R
import chat.tox.antox.activities.{CallActivity, MainActivity}
import chat.tox.antox.utils.{AntoxNotificationManager, Constants, NotificationOffsets}
import chat.tox.antox.wrapper.ContactInfo

class OngoingCallNotification(context: Context, contact: ContactInfo, call: Call) {

  //ensure that this id is not the same as is used for messages
  val id: Int = contact.key.hashCode() + NotificationOffsets.NOTIFICATION_OFFSET_ONGOING_CALL

  val builder = new NotificationCompat.Builder(context)
    .setSmallIcon(R.drawable.ic_actionbar)
    .setOngoing(true)
    .addAction(R.drawable.ic_call_end_white_24dp,
      context.getResources.getString(R.string.end_call),
      createPendingIntent(Constants.END_CALL, classOf[CallActivity], addParentStack = false)) // end call intent for button press
    .setContentText(context.getResources.getString(R.string.call_ongoing))
    .setContentTitle(contact.getDisplayName)
    .setUsesChronometer(true) // call timer in top right corner
    .setWhen(call.startTime.toMillis)
    .setSound(null)

  AntoxNotificationManager.addAvatarToNotification(builder, contact.key)

  builder.setContentIntent(createPendingIntent(Constants.SWITCH_TO_CALL, classOf[CallActivity], addParentStack = true))

  def createPendingIntent(action: String, activity: Class[_], addParentStack: Boolean): PendingIntent = {
    val resultIntent = new Intent(context, activity)
    resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
    resultIntent.setAction(action)
    resultIntent.putExtra("key", contact.key.toString)
    resultIntent.putExtra("call_number", call.callNumber.value)

    if (addParentStack) {
      val stackBuilder = TaskStackBuilder.create(context)
      stackBuilder.addParentStack(classOf[MainActivity])
      stackBuilder.addNextIntent(resultIntent)
      stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    } else {
      PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
  }

  def build(): Notification = {
    builder.build()
  }
}
