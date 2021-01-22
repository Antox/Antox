package chat.tox.antox.av

import android.app.Notification
import android.content.Context
import android.support.v4.app.NotificationCompat
import chat.tox.antox.R
import chat.tox.antox.activities.ChatActivity
import chat.tox.antox.utils.AntoxNotificationManager._
import chat.tox.antox.utils.TimestampUtils._
import chat.tox.antox.utils.{Constants, NotificationOffsets}
import chat.tox.antox.wrapper.{ContactInfo, Message, ToxKey}

object MissedCallNotification {
  //ensure that this id is not the same as is used for messages
  def id(key: ToxKey): Int = key.hashCode() + NotificationOffsets.NOTIFICATION_OFFSET_MISSED_CALL
}

class MissedCallNotification(context: Context, contact: ContactInfo, missedCallMessages: Seq[Message]) {

  val numMissedCallString = if (missedCallMessages.length > 1) s" (${missedCallMessages.length})" else ""

  val startCallPendingIntent = createChatPendingIntent(context, Constants.START_CALL, classOf[ChatActivity], contact.key)
  val replyPendingIntent = createChatPendingIntent(context, Constants.SWITCH_TO_FRIEND, classOf[ChatActivity], contact.key)

  val builder = new NotificationCompat.Builder(context)
    .setSmallIcon(R.drawable.ic_actionbar)
    .addAction(R.drawable.ic_call_white_24dp,
      context.getResources.getString(R.string.call_missed_call_back),
      startCallPendingIntent)
    .addAction(R.drawable.ic_send_white_24dp,
      context.getResources.getString(R.string.call_missed_reply),
      replyPendingIntent)
    .setContentTitle(context.getResources.getString(R.string.call_missed_title) + numMissedCallString)
    .setContentText(contact.getDisplayName)
    .setWhen(missedCallMessages.map(_.timestamp).max.getTime) // timestamp is the latest missed call message
    .setSound(null)

  addAvatarToNotification(builder, contact.key)

  builder.setContentIntent(replyPendingIntent)

  def build(): Notification = {
    builder.build()
  }
}
