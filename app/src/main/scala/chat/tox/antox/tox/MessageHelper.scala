package chat.tox.antox.tox

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.activities.{ChatActivity, GroupChatActivity, MainActivity}
import chat.tox.antox.data.State
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.MessageType.MessageType
import chat.tox.antox.wrapper.{MessageType, ToxKey}

import scala.collection.mutable.ArrayBuffer

object MessageHelper {

  val TAG = this.getClass.getSimpleName

  def createRequestNotification(contentText: Option[String], context: Context): Unit = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_friend_request", true)) {
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
      ToxSingleton.mNotificationManager.notify(0, mBuilder.build())
    }
  }

  def handleMessage(ctx: Context, friendNumber: Int, friendKey: ToxKey, message: String, messageType: MessageType): Unit = {
    val db = State.db
    val friendName = db.getContactNameOrAlias(friendKey)

    Log.d(TAG, "friend id: " + friendKey + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    if (!db.isContactBlocked(friendKey)) {
      val chatActive = State.chatActive && State.activeKey.contains(friendKey)
      db.addMessage(-1, friendKey, friendKey, friendName, message, hasBeenReceived = true,
        hasBeenRead = chatActive, successfullySent = true, messageType)

      if (!chatActive) {
        val unreadCount = db.getUnreadCounts(friendKey)
        val notificationContent =
          if (unreadCount > 1) {
            ctx.getResources.getString(R.string.unread_count, unreadCount.toString)
          } else {
            message
          }

        createMessageNotification(ctx, classOf[ChatActivity], friendNumber, friendKey, friendName, notificationContent)
      }
    }

  }

  def handleGroupMessage(ctx: Context, groupNumber: Int, peerNumber: Int, message: String, messageType: MessageType): Unit = {
    val db = State.db
    val peer = ToxSingleton.getGroupPeer(groupNumber, peerNumber)
    val group = ToxSingleton.getGroup(groupNumber)

    val chatActive = State.chatActive && State.activeKey.contains(group.key)

    db.addMessage(-1, group.key, peer.key, peer.name, message, hasBeenReceived = true,
      hasBeenRead = chatActive, successfullySent = true, messageType)

    if (!chatActive) {
      createMessageNotification(ctx, classOf[GroupChatActivity], groupNumber, group.key, group.name, message)
    }
  }

  def createMessageNotification(ctx: Context, intentClass: Class[_], number: Int, key: ToxKey, name: String, content: String): Unit = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val notificationsEnabled = preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_new_message", true)

    if (notificationsEnabled) {
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
      ToxSingleton.mNotificationManager.notify(number, mBuilder.build())
    }
  }

  def sendMessage(ctx: Context, key: ToxKey, msg: String, isAction: Boolean, mDbId: Option[Integer]): Unit = {
      val mFriend = ToxSingleton.getAntoxFriend(key)
      val messageType = if (isAction) MessageType.ACTION else MessageType.MESSAGE
      mFriend match {
        case None =>
        case Some(friend) =>
          val db = State.db
          for (splitMsg <- splitMessage(msg)) {
            val mId = try {
              Some(
                if (isAction) friend.sendAction(splitMsg) else friend.sendMessage(splitMsg)
                )
            } catch {
              case e: Exception =>
                None
            }

            val senderName = ToxSingleton.tox.getName
            val senderKey = ToxSingleton.tox.getSelfKey
            mId match {
              case Some(id) =>
                mDbId match {
                  case Some(dbId) => db.updateUnsentMessage(id, dbId)
                  case None => db.addMessage(id, key, senderKey, senderName,
                    splitMsg, hasBeenReceived =
                    false, hasBeenRead = false, successfullySent = true, messageType)
                }
              case None => db.addMessage(-1, key, senderKey, senderName, splitMsg, hasBeenReceived = false,
                hasBeenRead = false, successfullySent = false, messageType)
            }
          }
      }
  }

  def sendGroupMessage(ctx: Context, key: ToxKey, msg: String, isAction: Boolean, mDbId: Option[Integer]): Unit = {
    val group = ToxSingleton.getGroup(key)
    val db = State.db
    val messageType = if (isAction) MessageType.GROUP_ACTION else MessageType.GROUP_MESSAGE
    for (splitMsg <- splitMessage(msg)) {
      try {
        if (isAction) {
          group.sendAction(splitMsg)
        } else {
          group.sendMessage(splitMsg)
        }
      } catch {
        case e: Exception =>
          None
      }

      val senderKey = ToxSingleton.tox.getSelfKey
      val senderName = ToxSingleton.tox.getName
      mDbId match {
        case Some(dbId) => db.updateUnsentMessage(0, dbId)
        case None => db.addMessage(0, key, senderKey, senderName,
          splitMsg, hasBeenReceived =
            true, hasBeenRead = true, successfullySent = true, messageType)
      }
    }
  }

  def splitMessage(msg: String): Array[String] = {
    var currSplitPos = 0
    // convert the string to bytes to handle multibyte languages
    val message = msg.getBytes("UTF-8")
    val result: ArrayBuffer[String] = new ArrayBuffer[String]()

    while (message.length - currSplitPos > Constants.MAX_MESSAGE_LENGTH) {
      var pos = currSplitPos + Constants.MAX_MESSAGE_LENGTH

      // find the last whole unicode char
      while ((message(pos) & 0xc0) == 0x80) {
        pos -= 1
      }

      val str = new String(message.slice(currSplitPos, pos))
      result += str
      currSplitPos = pos
    }
    if (message.length - currSplitPos > 0) {
      result += new String(message.slice(currSplitPos, message.length))
    }

    result.toArray
  }

  def sendUnsentMessages(ctx: Context) {
    val db = State.db
    val unsentMessageList = db.getUnsentMessageList
    for (unsentMessage <- unsentMessageList) {
      val mFriend = ToxSingleton.getAntoxFriend(unsentMessage.key)
      mFriend.foreach(friend => {
        if (friend.isOnline && ToxSingleton.tox != null) {
          sendMessage(ctx, unsentMessage.key, unsentMessage.message,
            unsentMessage.`type` == MessageType.ACTION, Some(unsentMessage.id))
        }
      })
    }
  }
}
