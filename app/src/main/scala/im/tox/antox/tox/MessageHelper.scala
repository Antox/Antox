package im.tox.antox.tox

import java.util

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.app.{NotificationCompat, TaskStackBuilder}
import android.util.Log
import im.tox.antox.activities.MainActivity
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.utils.Constants
import im.tox.antox.wrapper.MessageType
import im.tox.antox.wrapper.MessageType.MessageType
import im.tox.antoxnightly.R

import scala.collection.JavaConverters._

object MessageHelper {

  val TAG = "im.tox.antox.tox.MessageHelper"

  def handleMessage(ctx: Context, friendNumber: Int, friendKey: String, message: String, messageType: MessageType): Unit = {
    val db = new AntoxDB(ctx)
    val friendName = db.getContactNameOrAlias(friendKey)

    Log.d(TAG, "friend id: " + friendKey + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    if (!db.isContactBlocked(friendKey)) {
      val chatActive = State.chatActive && State.activeKey.contains(friendKey)
      db.addMessage(-1, friendKey, friendName, message, has_been_received = true,
        has_been_read = chatActive, successfully_sent = true, messageType)

      ToxSingleton.updateMessages(ctx)
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
            val resultIntent = new Intent(ctx, classOf[MainActivity])
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
            resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
            resultIntent.putExtra("key", friendKey)
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

    db.close()
  }

  def handleGroupMessage(ctx: Context, groupNumber: Int, peerNumber: Int, groupId: String, message: String, messageType: MessageType) = {
    val db = new AntoxDB(ctx)
    val peerName = ToxSingleton.getGroupPeer(groupNumber, peerNumber).name

    val chatActive = State.chatActive && State.activeKey.contains(groupId)

    db.addMessage(-1, groupId, peerName, message, has_been_received = true,
      has_been_read = chatActive, successfully_sent = true, messageType)
    db.close()
    ToxSingleton.updateMessages(ctx)

    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val notificationsEnabled = preferences.getBoolean("notifications_enable_notifications", true) &&
                               preferences.getBoolean("notifications_new_message", true)

    if (!chatActive && notificationsEnabled) {
      val groupName = ToxSingleton.getGroup(groupNumber).name
      val mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.ic_actionbar)
        .setContentTitle(groupName)
        .setContentText(message)
        .setDefaults(Notification.DEFAULT_ALL)
      val resultIntent = new Intent(ctx, classOf[MainActivity])
      resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
      resultIntent.setAction(Constants.SWITCH_TO_FRIEND)
      resultIntent.putExtra("key", groupId)
      resultIntent.putExtra("name", groupName)
      val stackBuilder = TaskStackBuilder.create(ctx)
      stackBuilder.addParentStack(classOf[MainActivity])
      stackBuilder.addNextIntent(resultIntent)
      val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
      mBuilder.setContentIntent(resultPendingIntent)
      ToxSingleton.mNotificationManager.notify(groupNumber, mBuilder.build())
    }
  }

  def sendMessage(ctx: Context, key: String, msg: String, isAction: Boolean, mDbId: Option[Integer]) = {
      val mFriend = ToxSingleton.getAntoxFriend(key)
      val messageType = if (isAction) MessageType.ACTION else MessageType.OWN
      mFriend match {
        case None =>
        case Some(friend) =>
          val db = new AntoxDB(ctx)
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
                    splitMsg, has_been_received =
                    false, has_been_read = false, successfully_sent = true, messageType)
                }
              case None => db.addMessage(-1, key, senderName, splitMsg, has_been_received = false,
                has_been_read = false, successfully_sent = false, messageType)
            }
          }
          db.close()
          ToxSingleton.updateMessages(ctx)
      }
  }

  def sendGroupMessage(ctx: Context, key: String, msg: String, isAction: Boolean, mDbId: Option[Integer]) = {
    val group = ToxSingleton.getGroup(key)
    val db = new AntoxDB(ctx)
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
          splitMsg, has_been_received =
            true, has_been_read = true, successfully_sent = true, messageType)
      }
    }
    db.close()
    ToxSingleton.updateMessages(ctx)
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
    val db = new AntoxDB(ctx)
    val unsentMessageList = db.getUnsentMessageList
    db.close()
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
