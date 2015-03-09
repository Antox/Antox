package im.tox.antox.tox

import java.nio.{ByteBuffer, ByteOrder}
import java.util

import android.app.{PendingIntent, Notification}
import android.content.{Intent, Context}
import android.preference.PreferenceManager
import android.support.v4.app.{TaskStackBuilder, NotificationCompat}
import im.tox.antox.R
import im.tox.antox.activities.MainActivity
import im.tox.antox.utils.Constants
import android.util.Log
import im.tox.antox.data.{AntoxDB, State}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler
import collection.JavaConverters._

object MessageHelper {

  val TAG = "im.tox.antox.tox.MessageHandler"
  val MAX_MESSAGE_LENGTH = 1367

  def handleMessage(ctx: Context, friendNumber: Int, friendKey: String, rawMessage: String, messageType: Int): Unit = {
    val db = new AntoxDB(ctx)
    val friendName = db.getFriendNameOrAlias(friendKey)
    val message = if (messageType == Constants.MESSAGE_TYPE_ACTION) {
      formatAction(rawMessage, friendName)
    } else {
      rawMessage
    }

    Log.d(TAG, "friend id: " + friendKey + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    if (!db.isFriendBlocked(friendKey)) {
      val chatActive = (State.chatActive && State.activeKey.contains(friendKey))
      db.addMessage(-1, friendKey, message, friendName, has_been_received = true,
        has_been_read = chatActive, successfully_sent = true, messageType)
    }
    db.close()
    ToxSingleton.updateMessages(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    if (preferences.getBoolean("notifications_enable_notifications", true) &&
      preferences.getBoolean("notifications_new_message", true)) {
      if (!(State.chatActive && State.activeKey.contains(friendKey))) {
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

  def handleGroupMessage(ctx: Context, groupNumber: Int, peerNumber: Int, groupId: String, message: String, messageType: Int) = {
    println("handling group message")
    val db = new AntoxDB(ctx)
    val peerName = ToxSingleton.getGroupList.getByGroupNumber(groupNumber).get.peers.getByGroupPeerNumber(peerNumber).name

    println("group id: " + groupId + " activeKey: " + State.activeKey + " chatActive: " + State.chatActive)
    val chatActive = State.chatActive && State.activeKey.contains(groupId)

    db.addMessage(-1, groupId, message, peerName, has_been_received = true,
      has_been_read = chatActive, successfully_sent = true, messageType)
    db.close()
    ToxSingleton.updateMessages(ctx)
  }

  def formatAction(action: String, friendName: String): String = {
    var formattedAction = ""
    if (!action.startsWith(friendName)) {
      formattedAction = friendName + " " + action
    }

    formattedAction
  }

  def sendMessage(ctx: Context, key: String, msg: String, mDbId: Option[Integer]) = {
      val mFriend = ToxSingleton.getAntoxFriend(key)
      mFriend match {
        case None =>
        case Some(friend) => {
          val db = new AntoxDB(ctx).open(writeable = true)
          for (splitMsg <- splitMessage(msg)) {
            val mId = try {
              println("sent message of length " + splitMsg.length)
              Some(ToxSingleton.tox.sendMessage(friend.getFriendnumber, splitMsg))
            } catch {
              case e: Exception => {
                None
              }
            }

            val senderName = db.getFriendNameOrAlias(key)
            mId match {
              case Some(id) => {
                mDbId match {
                  case Some(dbId) => db.updateUnsentMessage(id, dbId)
                  case None => db.addMessage(id, key, senderName,
                    splitMsg, has_been_received =
                    false, has_been_read = false, successfully_sent = true, 1)
                }
              }
              case None => db.addMessage(-1, key, senderName, splitMsg, has_been_received = false,
                has_been_read = false, successfully_sent = false, 1)
            }
          }
          db.close()
          ToxSingleton.updateMessages(ctx)
        }
      }
  }

  def sendGroupMessage(ctx: Context, key: String, msg: String, mDbId: Option[Integer]) = {
    val mGroup = ToxSingleton.getGroupList.getByGroupId(key)
    mGroup match {
      case None =>
      case Some(group) => {
        val db = new AntoxDB(ctx).open(writeable = true)
        for (splitMsg <- splitMessage(msg)) {
          try {
            println("sent message of length " + splitMsg.length)
            ToxSingleton.tox.sendGroupMessage(group.groupNumber, splitMsg)
          } catch {
            case e: Exception => {
              None
            }
          }

          val senderName = db.getGroupNameOrAlias(key)
          mDbId match {
            case Some(dbId) => db.updateUnsentMessage(0, dbId)
            case None => db.addMessage(0, key, senderName,
              splitMsg, has_been_received =
                true, has_been_read = true, successfully_sent = true, Constants.MESSAGE_TYPE_GROUP_OWN)
          }
        }
        db.close()
        ToxSingleton.updateMessages(ctx)
      }
    }
  }

  def splitMessage(msg: String): Array[String] = {
    var currSplitPos = 0
    val result: util.ArrayList[String] = new util.ArrayList[String]()

    while (msg.length - currSplitPos > MAX_MESSAGE_LENGTH) {
      val str = msg.substring(currSplitPos, currSplitPos + MAX_MESSAGE_LENGTH)
      val spacePos = str.lastIndexOf(' ')

      if (spacePos <= 0) {
        result.add(str)
        currSplitPos += MAX_MESSAGE_LENGTH
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
    val db = new AntoxDB(ctx).open(writeable = false)
    val unsentMessageList = db.getUnsentMessageList
    db.close()
    for (unsentMessage <- unsentMessageList) {
      val mFriend = ToxSingleton.getAntoxFriend(unsentMessage.key)
      mFriend.foreach(friend => {
        if (friend.isOnline && ToxSingleton.tox != null) {
          sendMessage(ctx, unsentMessage.key, unsentMessage.message, Some(unsentMessage.id))
        }
      })
    }
  }
}
