package im.tox.tox4j.impl.jni

import im.tox.tox4j.OptimisedIdOps._
import im.tox.tox4j.core.callbacks.ToxCoreEventListener
import im.tox.tox4j.core.data._
import im.tox.tox4j.core.enums.{ ToxConnection, ToxFileControl, ToxMessageType, ToxUserStatus }
import im.tox.tox4j.core.proto._
import org.jetbrains.annotations.Nullable

object ToxCoreEventDispatch {

  def convert(status: Connection.Type): ToxConnection = {
    status match {
      case Connection.Type.NONE => ToxConnection.NONE
      case Connection.Type.TCP => ToxConnection.TCP
      case Connection.Type.UDP => ToxConnection.UDP
    }
  }

  def convert(status: UserStatus.Type): ToxUserStatus = {
    status match {
      case UserStatus.Type.NONE => ToxUserStatus.NONE
      case UserStatus.Type.AWAY => ToxUserStatus.AWAY
      case UserStatus.Type.BUSY => ToxUserStatus.BUSY
    }
  }

  def convert(status: ToxUserStatus): UserStatus.Type = {
    status match {
      case ToxUserStatus.NONE => UserStatus.Type.NONE
      case ToxUserStatus.AWAY => UserStatus.Type.AWAY
      case ToxUserStatus.BUSY => UserStatus.Type.BUSY
    }
  }

  def convert(control: FileControl.Type): ToxFileControl = {
    control match {
      case FileControl.Type.RESUME => ToxFileControl.RESUME
      case FileControl.Type.PAUSE => ToxFileControl.PAUSE
      case FileControl.Type.CANCEL => ToxFileControl.CANCEL
    }
  }

  def convert(messageType: MessageType.Type): ToxMessageType = {
    messageType match {
      case MessageType.Type.NORMAL => ToxMessageType.NORMAL
      case MessageType.Type.ACTION => ToxMessageType.ACTION
    }
  }

  private def dispatchSelfConnectionStatus[S](handler: ToxCoreEventListener[S], selfConnectionStatus: Seq[SelfConnectionStatus])(state: S): S = {
    selfConnectionStatus.foldLeft(state) {
      case (state, SelfConnectionStatus(status)) =>
        handler.selfConnectionStatus(
          convert(status)
        )(state)
    }
  }

  private def dispatchFriendName[S](handler: ToxCoreEventListener[S], friendName: Seq[FriendName])(state: S): S = {
    friendName.foldLeft(state) {
      case (state, FriendName(friendNumber, name)) =>
        handler.friendName(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          ToxNickname.unsafeFromValue(name.toByteArray)
        )(state)
    }
  }

  private def dispatchFriendStatusMessage[S](handler: ToxCoreEventListener[S], friendStatusMessage: Seq[FriendStatusMessage])(state: S): S = {
    friendStatusMessage.foldLeft(state) {
      case (state, FriendStatusMessage(friendNumber, message)) =>
        handler.friendStatusMessage(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          ToxStatusMessage.unsafeFromValue(message.toByteArray)
        )(state)
    }
  }

  private def dispatchFriendStatus[S](handler: ToxCoreEventListener[S], friendStatus: Seq[FriendStatus])(state: S): S = {
    friendStatus.foldLeft(state) {
      case (state, FriendStatus(friendNumber, status)) =>
        handler.friendStatus(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          convert(status)
        )(state)
    }
  }

  private def dispatchFriendConnectionStatus[S](handler: ToxCoreEventListener[S], friendConnectionStatus: Seq[FriendConnectionStatus])(state: S): S = {
    friendConnectionStatus.foldLeft(state) {
      case (state, FriendConnectionStatus(friendNumber, status)) =>
        handler.friendConnectionStatus(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          convert(status)
        )(state)
    }
  }

  private def dispatchFriendTyping[S](handler: ToxCoreEventListener[S], friendTyping: Seq[FriendTyping])(state: S): S = {
    friendTyping.foldLeft(state) {
      case (state, FriendTyping(friendNumber, isTyping)) =>
        handler.friendTyping(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          isTyping
        )(state)
    }
  }

  private def dispatchFriendReadReceipt[S](handler: ToxCoreEventListener[S], friendReadReceipt: Seq[FriendReadReceipt])(state: S): S = {
    friendReadReceipt.foldLeft(state) {
      case (state, FriendReadReceipt(friendNumber, messageId)) =>
        handler.friendReadReceipt(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          messageId
        )(state)
    }
  }

  private def dispatchFriendRequest[S](handler: ToxCoreEventListener[S], friendRequest: Seq[FriendRequest])(state: S): S = {
    friendRequest.foldLeft(state) {
      case (state, FriendRequest(publicKey, timeDelta, message)) =>
        handler.friendRequest(
          ToxPublicKey.unsafeFromValue(publicKey.toByteArray),
          timeDelta,
          ToxFriendRequestMessage.unsafeFromValue(message.toByteArray)
        )(state)
    }
  }

  private def dispatchFriendMessage[S](handler: ToxCoreEventListener[S], friendMessage: Seq[FriendMessage])(state: S): S = {
    friendMessage.foldLeft(state) {
      case (state, FriendMessage(friendNumber, messageType, timeDelta, message)) =>
        handler.friendMessage(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          convert(messageType),
          timeDelta,
          ToxFriendMessage.unsafeFromValue(message.toByteArray)
        )(state)
    }
  }

  private def dispatchFileRecvControl[S](handler: ToxCoreEventListener[S], fileRecvControl: Seq[FileRecvControl])(state: S): S = {
    fileRecvControl.foldLeft(state) {
      case (state, FileRecvControl(friendNumber, fileNumber, control)) =>
        handler.fileRecvControl(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          fileNumber,
          convert(control)
        )(state)
    }
  }

  private def dispatchFileChunkRequest[S](handler: ToxCoreEventListener[S], fileChunkRequest: Seq[FileChunkRequest])(state: S): S = {
    fileChunkRequest.foldLeft(state) {
      case (state, FileChunkRequest(friendNumber, fileNumber, position, length)) =>
        handler.fileChunkRequest(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          fileNumber,
          position,
          length
        )(state)
    }
  }

  private def dispatchFileRecv[S](handler: ToxCoreEventListener[S], fileRecv: Seq[FileRecv])(state: S): S = {
    fileRecv.foldLeft(state) {
      case (state, FileRecv(friendNumber, fileNumber, kind, fileSize, filename)) =>
        handler.fileRecv(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          fileNumber,
          kind,
          fileSize,
          ToxFilename.unsafeFromValue(filename.toByteArray)
        )(state)
    }
  }

  private def dispatchFileRecvChunk[S](handler: ToxCoreEventListener[S], fileRecvChunk: Seq[FileRecvChunk])(state: S): S = {
    fileRecvChunk.foldLeft(state) {
      case (state, FileRecvChunk(friendNumber, fileNumber, position, data)) =>
        handler.fileRecvChunk(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          fileNumber,
          position,
          data.toByteArray
        )(state)
    }
  }

  private def dispatchFriendLossyPacket[S](handler: ToxCoreEventListener[S], friendLossyPacket: Seq[FriendLossyPacket])(state: S): S = {
    friendLossyPacket.foldLeft(state) {
      case (state, FriendLossyPacket(friendNumber, data)) =>
        handler.friendLossyPacket(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          ToxLossyPacket.unsafeFromValue(data.toByteArray)
        )(state)
    }
  }

  private def dispatchFriendLosslessPacket[S](handler: ToxCoreEventListener[S], friendLosslessPacket: Seq[FriendLosslessPacket])(state: S): S = {
    friendLosslessPacket.foldLeft(state) {
      case (state, FriendLosslessPacket(friendNumber, data)) =>
        handler.friendLosslessPacket(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          ToxLosslessPacket.unsafeFromValue(data.toByteArray)
        )(state)
    }
  }

  private def dispatchEvents[S](handler: ToxCoreEventListener[S], events: CoreEvents)(state: S): S = {
    (state
      |> dispatchSelfConnectionStatus(handler, events.selfConnectionStatus)
      |> dispatchFriendName(handler, events.friendName)
      |> dispatchFriendStatusMessage(handler, events.friendStatusMessage)
      |> dispatchFriendStatus(handler, events.friendStatus)
      |> dispatchFriendConnectionStatus(handler, events.friendConnectionStatus)
      |> dispatchFriendTyping(handler, events.friendTyping)
      |> dispatchFriendReadReceipt(handler, events.friendReadReceipt)
      |> dispatchFriendRequest(handler, events.friendRequest)
      |> dispatchFriendMessage(handler, events.friendMessage)
      |> dispatchFileRecvControl(handler, events.fileRecvControl)
      |> dispatchFileChunkRequest(handler, events.fileChunkRequest)
      |> dispatchFileRecv(handler, events.fileRecv)
      |> dispatchFileRecvChunk(handler, events.fileRecvChunk)
      |> dispatchFriendLossyPacket(handler, events.friendLossyPacket)
      |> dispatchFriendLosslessPacket(handler, events.friendLosslessPacket))
  }

  @SuppressWarnings(Array(
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.Null"
  ))
  def dispatch[S](handler: ToxCoreEventListener[S], @Nullable eventData: Array[Byte])(state: S): S = {
    if (eventData == null) { // scalastyle:ignore null
      state
    } else {
      val events = CoreEvents.parseFrom(eventData)
      dispatchEvents(handler, events)(state)
    }
  }

}
