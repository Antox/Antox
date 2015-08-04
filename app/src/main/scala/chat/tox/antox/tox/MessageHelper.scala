package chat.tox.antox.tox

import java.util

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.activities.{GroupChatActivity, ChatActivity, MainActivity}
import chat.tox.antox.data.State
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.MessageType.MessageType
import chat.tox.antox.wrapper.{MessageType, ToxKey}

import scala.collection.JavaConverters._

object MessageHelper {

  val TAG = "chat.tox.antox.tox.MessageHelper"

  def handleMessage(ctx: Context, friendNumber: Int, friendKey: ToxKey, message: String, messageType: MessageType): Unit = {
    val db = State.db
    val friendName = db.getContactNameOrAlias(friendKey)

    Log.d(TAG, "friend id: " + friendKey + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    if (!db.isContactBlocked(friendKey)) {
      val chatActive = State.chatActive && State.activeKey.contains(friendKey)
      db.addMessage(-1, friendKey, friendName, message, hasBeenReceived = true,
        hasBeenRead = chatActive, successfullySent = true, messageType)

      val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
      if (preferences.getBoolean("notifications_enable_notifications", true) &&
        preferences.getBoolean("notifications_new_message", true)) {
        if (!chatActive) {
          val mName = ToxSingleton.getAntoxFriend(friendKey).map(_.getName)
          mName.foreach(name => {
            val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
              .setContentTitle(name)
              .setContentText(message)
              .setDefaults(Notification.DEFAULT_ALL)
            val resultIntent = new Intent(ctx, classOf[ChatActivity])
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
            resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
            resultIntent.putExtra("key", friendKey.toString)
            resultIntent.putExtra("name", name)
            val stackBuilder = TaskStackBuilder.create(ctx)
            stackBuilder.addParentStack(classOf[MainActivity])
            stackBuilder.addNextIntent(resultIntent)
            val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            mBuilder.setContentIntent(resultPendingIntent)
            ToxSingleton.mNotificationManager.notify(friendNumber, mBuilder.build())
          })
        }
      }
    }

  }

  def handleGroupMessage(ctx: Context, groupNumber: Int, peerNumber: Int, groupKey: ToxKey, message: String, messageType: MessageType) = {
    val db = State.db
    val peerName = ToxSingleton.getGroupPeer(groupNumber, peerNumber).name

    val chatActive = State.chatActive && State.activeKey.contains(groupKey)

    db.addMessage(-1, groupKey, peerName, message, hasBeenReceived = true,
      hasBeenRead = chatActive, successfullySent = true, messageType)

    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val notificationsEnabled = preferences.getBoolean("notifications_enable_notifications", true) &&
                               preferences.getBoolean("notifications_new_message", true)

    if (!chatActive && notificationsEnabled) {
      val groupName = ToxSingleton.getGroup(groupNumber).name
      val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(groupName)
        .setContentText(message)
        .setDefaults(Notification.DEFAULT_ALL)
      val resultIntent = new Intent(ctx, classOf[GroupChatActivity])
      resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
      resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
      resultIntent.putExtra("key", groupKey.toString)
      resultIntent.putExtra("name", groupName)
      val stackBuilder = TaskStackBuilder.create(ctx)
      stackBuilder.addParentStack(classOf[MainActivity])
      stackBuilder.addNextIntent(resultIntent)
      val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
      mBuilder.setContentIntent(resultPendingIntent)
      ToxSingleton.mNotificationManager.notify(groupNumber, mBuilder.build())
    }
  }

  def sendMessage(ctx: Context, key: ToxKey, msg: String, isAction: Boolean, mDbId: Option[Integer]) = {
      val mFriend = ToxSingleton.getAntoxFriend(key)
      val messageType = if (isAction) MessageType.ACTION else MessageType.OWN
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
            mId match {
              case Some(id) =>
                mDbId match {
                  case Some(dbId) => db.updateUnsentMessage(id, dbId)
                  case None => db.addMessage(id, key, senderName,
                    splitMsg, hasBeenReceived =
                    false, hasBeenRead = false, successfullySent = true, messageType)
                }
              case None => db.addMessage(-1, key, senderName, splitMsg, hasBeenReceived = false,
                hasBeenRead = false, successfullySent = false, messageType)
            }
          }
      }
  }

  def sendGroupMessage(ctx: Context, key: ToxKey, msg: String, isAction: Boolean, mDbId: Option[Integer]) = {
    val group = ToxSingleton.getGroup(key)
    val db = State.db
    val messageType = if (isAction) MessageType.GROUP_ACTION else MessageType.GROUP_OWN
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

      val senderName = ToxSingleton.tox.getName
      mDbId match {
        case Some(dbId) => db.updateUnsentMessage(0, dbId)
        case None => db.addMessage(0, key, senderName,
          splitMsg, hasBeenReceived =
            true, hasBeenRead = true, successfullySent = true, messageType)
      }
    }
  }

  def splitMessage(msg: String): Array[String] = {
    var currSplitPos = 0
    val result: util.ArrayList[String] = new util.ArrayList[String]()

    while (msg.length - currSplitPos > Constants.MAX_MESSAGE_LENGTH) {
      val str = msg.substring(currSplitPos, currSplitPos + Constants.MAX_MESSAGE_LENGTH)
      val spacePos = str.lastIndexOf(' ')

      if (spacePos <= 0) {
        result.add(str)
        currSplitPos += Constants.MAX_MESSAGE_LENGTH
      } else {
        result.add(str.substring(0, spacePos))
        currSplitPos += spacePos + 1
      }
    }
    if (msg.length - currSplitPos > 0) {
      result.add(msg.substring(currSplitPos))
    }

    result.asScala.toArray
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
