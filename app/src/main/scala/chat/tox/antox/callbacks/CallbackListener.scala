package chat.tox.antox.callbacks

import android.content.Context
import im.tox.tox4j.core.callbacks.ToxEventListener
import im.tox.tox4j.core.enums.{ToxConnection, ToxFileControl, ToxMessageType, ToxUserStatus}

class CallbackListener(ctx: Context) extends ToxEventListener[Unit] {
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

  override def friendTyping(friendNumber: Int, isTyping: Boolean)(state: Unit): Unit =
    typingChangeCallback.friendTyping(friendNumber, isTyping)(Unit)

  override def fileRecvChunk(friendNumber: Int, fileNumber: Int, position: Long, data: Array[Byte])(state: Unit): Unit =
    fileRecvChunkCallback.fileRecvChunk(friendNumber, fileNumber, position, data)(Unit)

  override def fileRecvControl(friendNumber: Int, fileNumber: Int, control: ToxFileControl)(state: Unit): Unit =
    fileRecvControlCallback.fileRecvControl(friendNumber, fileNumber, control)(Unit)

  override def friendConnectionStatus(friendNumber: Int, connectionStatus: ToxConnection)(state: Unit): Unit =
    connectionStatusCallback.friendConnectionStatus(friendNumber, connectionStatus)(Unit)

  override def friendLosslessPacket(friendNumber: Int, data: Array[Byte])(state: Unit): Unit =
    friendLosslessPacketCallback.friendLosslessPacket(friendNumber, data)(Unit)

  override def friendReadReceipt(friendNumber: Int, messageId: Int)(state: Unit): Unit =
    readReceiptCallback.friendReadReceipt(friendNumber, messageId)(Unit)

  override def fileChunkRequest(friendNumber: Int, fileNumber: Int, position: Long, length: Int)(state: Unit): Unit =
    fileChunkRequestCallback.fileChunkRequest(friendNumber, fileNumber, position, length)(Unit)

  override def friendStatusMessage(friendNumber: Int, message: Array[Byte])(state: Unit): Unit =
    statusMessageCallback.friendStatusMessage(friendNumber, message)(Unit)

  override def friendStatus(friendNumber: Int, status: ToxUserStatus)(state: Unit): Unit =
    userStatusCallback.friendStatus(friendNumber, status)(Unit)

  override def friendMessage(friendNumber: Int, messageType: ToxMessageType, timeDelta: Int, message: Array[Byte])(state: Unit): Unit =
    messageCallback.friendMessage(friendNumber, messageType, timeDelta, message)(Unit)

  override def fileRecv(friendNumber: Int, fileNumber: Int, kind: Int, fileSize: Long, filename: Array[Byte])(state: Unit): Unit =
    fileRecvCallback.fileRecv(friendNumber, fileNumber, kind, fileSize, filename)(Unit)

  override def selfConnectionStatus(connectionStatus: ToxConnection)(state: Unit): Unit =
    selfConnectionStatusCallback.selfConnectionStatus(connectionStatus)(Unit)

  override def friendName(friendNumber: Int, name: Array[Byte])(state: Unit): Unit =
    nameChangeCallback.friendName(friendNumber, name)(Unit)

  override def friendRequest(publicKey: Array[Byte], timeDelta: Int, message: Array[Byte])(state: Unit): Unit =
    friendRequestCallback.friendRequest(publicKey, timeDelta, message)(Unit)

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