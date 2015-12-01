package chat.tox.antox.tox

import android.content.Context
import chat.tox.antox.activities.{ChatActivity, GroupChatActivity}
import chat.tox.antox.data.State
import chat.tox.antox.utils._
import chat.tox.antox.wrapper._
import im.tox.tox4j.core.data.{ToxNickname, ToxFriendMessage}
import im.tox.tox4j.core.enums.ToxMessageType
import org.scaloid.common.LoggerTag

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object MessageHelper {

  val TAG = LoggerTag("MessageHelper")

  def handleMessage(ctx: Context, friendInfo: FriendInfo, message: ToxFriendMessage, messageType: ToxMessageType): Unit = {
    val db = State.db

    AntoxLog.debug(s"Message from: friend id: ${friendInfo.key} activeKey: ${State.activeKey} chatActive: ${State.chatActive}", TAG)

    if (!db.isContactBlocked(friendInfo.key)) {
      val chatActive = State.isChatActive(friendInfo.key)
      db.addMessage(friendInfo.key, friendInfo.key, ToxNickname.unsafeFromValue(friendInfo.getDisplayName.getBytes), new String(message.value), hasBeenReceived = true,
        hasBeenRead = chatActive, successfullySent = true, messageType)

      if (!chatActive) {
        val unreadCount = db.getUnreadCounts(friendInfo.key)
        AntoxNotificationManager.createMessageNotification(ctx, classOf[ChatActivity], friendInfo.key, friendInfo.name, new String(message.value), unreadCount)
      }
    }
  }

  def handleGroupMessage(ctx: Context, groupInfo: GroupInfo, peerInfo: GroupPeer, message: String, messageType: ToxMessageType): Unit = {
    val db = State.db

    val chatActive = State.isChatActive(groupInfo.key)

    db.addMessage(groupInfo.key, peerInfo.key, peerInfo.name, message, hasBeenReceived = true,
      hasBeenRead = chatActive, successfullySent = true, messageType)

    if (!chatActive) {
      AntoxNotificationManager.createMessageNotification(ctx, classOf[GroupChatActivity], groupInfo.key, groupInfo.name, message)
    }
  }

  def sendMessage(ctx: Context, friendKey: FriendKey, msg: String, messageType: ToxMessageType, mDbId: Option[Int]): Unit = {
    val db = State.db
    for (splitMsg <- splitMessage(msg)) {
      val mId = Try(ToxSingleton.tox.friendSendMessage(friendKey, ToxFriendMessage.unsafeFromValue(msg.getBytes), messageType)).toOption

      val senderName = ToxSingleton.tox.getName
      val senderKey = ToxSingleton.tox.getSelfKey
      mId match {
        case Some(id) =>
          mDbId match {
            case Some(dbId) => db.updateUnsentMessage(id, dbId)
            case None => db.addMessage(friendKey, senderKey, senderName,
              splitMsg, hasBeenReceived = false, hasBeenRead = false, successfullySent = true, messageType, messageId = id)
          }
        case None => db.addMessage(friendKey, senderKey, senderName, splitMsg, hasBeenReceived = false,
          hasBeenRead = false, successfullySent = false, messageType)
      }
    }
  }

  def sendGroupMessage(ctx: Context, groupKey: GroupKey, msg: String, messageType: ToxMessageType, mDbId: Option[Int]): Unit = {
    val db = State.db

    for (splitMsg <- splitMessage(msg)) {
      messageType match {
        case ToxMessageType.ACTION =>
          ToxSingleton.tox.sendGroupAction(groupKey, msg)
        case _ =>
          ToxSingleton.tox.sendGroupMessage(groupKey, msg)
      }

      val senderKey = ToxSingleton.tox.getSelfKey
      val senderName = ToxSingleton.tox.getName
      mDbId match {
        case Some(dbId) => db.updateUnsentMessage(0, dbId)
        case None => db.addMessage(groupKey, senderKey, senderName,
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

  def sendUnsentMessages(contactKey: ContactKey, ctx: Context) {
    val db = State.db
    val unsentMessageList = db.getUnsentMessageList(contactKey)

    AntoxLog.debug(s"Sending ${unsentMessageList.length} unsent messages.", TAG)

    for (unsentMessage <- unsentMessageList) {
        contactKey match {
          case key: FriendKey =>
            sendMessage(ctx, key, unsentMessage.message,
              MessageType.toToxMessageType(unsentMessage.`type`), Some(unsentMessage.id))
          case key: GroupKey =>
            sendGroupMessage(ctx, key, unsentMessage.message,
              MessageType.toToxMessageType(unsentMessage.`type`), Some(unsentMessage.id))
        }
    }
  }
}
