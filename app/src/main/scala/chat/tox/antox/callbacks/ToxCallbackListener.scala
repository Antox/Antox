package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.ToxCoreEventListener
import im.tox.tox4j.core.data._
import im.tox.tox4j.core.enums.{ToxConnection, ToxFileControl, ToxMessageType, ToxUserStatus}

class ToxCallbackListener(ctx: Context) extends ToxCoreEventListener[Unit] {
  val selfConnectionStatusCallback = new AntoxOnSelfConnectionStatusCallback(ctx)
  val messageCallback = new AntoxOnMessageCallback(ctx)
  val friendRequestCallback = new AntoxOnFriendRequestCallback(ctx)
  val connectionStatusCallback = new AntoxOnConnectionStatusCallback(ctx)
  val nameChangeCallback = new AntoxOnNameChangeCallback(ctx)
  val readReceiptCallback = new AntoxOnReadReceiptCallback(ctx)
  val statusMessageCallback = new AntoxOnStatusMessageCallback(ctx)
  val userStatusCallback = new AntoxOnUserStatusCallback(ctx)
  val typingChangeCallback = new AntoxOnTypingChangeCallback(ctx)
  val fileRecvCallback = new AntoxOnFileRecvCallback(ctx)
  val fileRecvChunkCallback = new AntoxOnFileRecvChunkCallback(ctx)
  val fileChunkRequestCallback = new AntoxOnFileChunkRequestCallback(ctx)
  val fileRecvControlCallback = new AntoxOnFileRecvControlCallback(ctx)
  val friendLosslessPacketCallback = new AntoxOnFriendLosslessPacketCallback(ctx)

  override def friendTyping(friendNumber: ToxFriendNumber, isTyping: Boolean)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    typingChangeCallback.friendTyping(friendInfo, isTyping)(Unit)
  }

  override def fileRecvChunk(friendNumber: ToxFriendNumber, fileNumber: Int, position: Long, data: Array[Byte])(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    fileRecvChunkCallback.fileRecvChunk(friendInfo, fileNumber, position, data)(Unit)
  }

  override def fileRecvControl(friendNumber: ToxFriendNumber, fileNumber: Int, control: ToxFileControl)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    fileRecvControlCallback.fileRecvControl(friendInfo, fileNumber, control)(Unit)
  }

  override def friendConnectionStatus(friendNumber: ToxFriendNumber, connectionStatus: ToxConnection)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    connectionStatusCallback.friendConnectionStatus(friendInfo, connectionStatus)(Unit)
  }

  override def friendLosslessPacket(friendNumber: ToxFriendNumber, data: ToxLosslessPacket)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    friendLosslessPacketCallback.friendLosslessPacket(friendInfo, data)(Unit)
  }

  override def friendReadReceipt(friendNumber: ToxFriendNumber, messageId: Int)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    readReceiptCallback.friendReadReceipt(friendInfo, messageId)(Unit)
  }

  override def fileChunkRequest(friendNumber: ToxFriendNumber, fileNumber: Int, position: Long, length: Int)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    fileChunkRequestCallback.fileChunkRequest(friendInfo, fileNumber, position, length)(Unit)
  }

  override def friendStatusMessage(friendNumber: ToxFriendNumber, message: ToxStatusMessage)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    statusMessageCallback.friendStatusMessage(friendInfo, message)(Unit)
  }

  override def friendStatus(friendNumber: ToxFriendNumber, status: ToxUserStatus)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    userStatusCallback.friendStatus(friendInfo, status)(Unit)
  }

  override def friendMessage(friendNumber: ToxFriendNumber, messageType: ToxMessageType, timeDelta: Int, message: ToxFriendMessage)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    messageCallback.friendMessage(friendInfo, messageType, timeDelta, message)(Unit)
  }

  override def fileRecv(friendNumber: ToxFriendNumber, fileNumber: Int, kind: Int, fileSize: Long, filename: ToxFilename)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    fileRecvCallback.fileRecv(friendInfo, fileNumber, kind, fileSize, filename)(Unit)
  }

  override def selfConnectionStatus(connectionStatus: ToxConnection)(state: Unit): Unit = {
    selfConnectionStatusCallback.selfConnectionStatus(connectionStatus)(Unit)
  }

  override def friendName(friendNumber: ToxFriendNumber, name: ToxNickname)(state: Unit): Unit = {
    val friendInfo = State.db.getFriendInfo(ToxSingleton.tox.getFriendKey(friendNumber))
    nameChangeCallback.friendName(friendInfo, name)(Unit)
  }

  override def friendRequest(publicKey: ToxPublicKey, timeDelta: Int, message: ToxFriendRequestMessage)(state: Unit): Unit = {
    friendRequestCallback.friendRequest(publicKey, timeDelta, message)(Unit)
  }

  /*
  val groupTopicChangeCallback = new AntoxOnGroupTopicChangeCallback(ctx)
  val groupPeerJoinCallback new AntoxOnPeerJoinCallback(ctx)
  val groupPeerExitCallback = new AntoxOnPeerExitCallback(ctx)
  val groupPeerlistUpdateCallback = new AntoxOnGroupPeerlistUpdateCallback(ctx)
  val groupNickChangeCallback = new AntoxOnGroupNickChangeCallback(ctx)
  val groupInviteCallback = new AntoxOnGroupInviteCallback(ctx)
  val groupSelfJoinCallback = new AntoxOnGroupSelfJoinCallback(ctx)
  val groupJoinRejectedCallback = new AntoxOnGroupJoinRejectedCallback(ctx)
  val groupSelfTimeoutCallback = new AntoxOnGroupSelfTimeoutCallback(ctx)
  val groupMessageCallback = new AntoxOnGroupMessageCallback(ctx) */
}