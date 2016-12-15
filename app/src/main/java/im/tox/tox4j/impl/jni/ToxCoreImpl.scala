package im.tox.tox4j.impl.jni

import com.typesafe.scalalogging.Logger
import im.tox.core.network.Port
import im.tox.tox4j.core._
import im.tox.tox4j.core.callbacks._
import im.tox.tox4j.core.data._
import im.tox.tox4j.core.enums.{ ToxConnection, ToxFileControl, ToxMessageType, ToxUserStatus }
import im.tox.tox4j.core.exceptions._
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.impl.jni.ToxCoreImpl.logger
import im.tox.tox4j.impl.jni.internal.Event
import org.jetbrains.annotations.{ NotNull, Nullable }
import org.slf4j.LoggerFactory

// scalastyle:off null
@SuppressWarnings(Array("org.wartremover.warts.Null"))
object ToxCoreImpl {

  private val logger = Logger(LoggerFactory.getLogger(getClass))

  @throws[ToxBootstrapException]
  private def checkBootstrapArguments(port: Int, @Nullable publicKey: Array[Byte]): Unit = {
    if (port < 0) {
      throw new ToxBootstrapException(ToxBootstrapException.Code.BAD_PORT, "Port cannot be negative")
    }
    if (port > 65535) {
      throw new ToxBootstrapException(ToxBootstrapException.Code.BAD_PORT, "Port cannot exceed 65535")
    }
    if (publicKey ne null) {
      if (publicKey.length < ToxCoreConstants.PublicKeySize) {
        throw new ToxBootstrapException(ToxBootstrapException.Code.BAD_KEY, "Key too short")
      }
      if (publicKey.length > ToxCoreConstants.PublicKeySize) {
        throw new ToxBootstrapException(ToxBootstrapException.Code.BAD_KEY, "Key too long")
      }
    }
  }

  private def throwLengthException(name: String, message: String, expectedSize: Int): Unit = {
    throw new IllegalArgumentException(s"$name too $message, must be $expectedSize bytes")
  }

  private def checkLength(name: String, @Nullable bytes: Array[Byte], expectedSize: Int): Unit = {
    if (bytes ne null) {
      if (bytes.length < expectedSize) {
        throwLengthException(name, "short", expectedSize)
      }
      if (bytes.length > expectedSize) {
        throwLengthException(name, "long", expectedSize)
      }
    }
  }

  @throws[ToxSetInfoException]
  private def checkInfoNotNull(info: Array[Byte]): Unit = {
    if (info eq null) {
      throw new ToxSetInfoException(ToxSetInfoException.Code.NULL)
    }
  }

}

/**
 * Initialises the new Tox instance with an optional save-data received from [[getSavedata]].
 *
 * @param options Connection options object with optional save-data.
 */
// scalastyle:off no.finalize number.of.methods
@throws[ToxNewException]("If an error was detected in the configuration or a runtime error occurred.")
final class ToxCoreImpl(@NotNull val options: ToxOptions) extends ToxCore {

  private[this] val onCloseCallbacks = new Event

  /**
   * This field has package visibility for [[ToxAvImpl]].
   */
  private[jni] val instanceNumber =
    ToxCoreJni.toxNew(
      options.ipv6Enabled,
      options.udpEnabled,
      options.proxy.proxyType.ordinal,
      options.proxy.proxyAddress,
      options.proxy.proxyPort,
      options.startPort,
      options.endPort,
      options.tcpPort,
      options.saveData.kind.ordinal,
      options.saveData.data
    )

  /**
   * Add an onClose callback. This event is invoked just before the instance is closed.
   */
  def addOnCloseCallback(callback: () => Unit): Event.Id =
    onCloseCallbacks += callback

  def removeOnCloseCallback(id: Event.Id): Unit =
    onCloseCallbacks -= id

  override def load(options: ToxOptions): ToxCoreImpl =
    new ToxCoreImpl(options)

  override def close(): Unit = {
    onCloseCallbacks()
    ToxCoreJni.toxKill(instanceNumber)
  }

  protected override def finalize(): Unit = {
    try {
      close()
      ToxCoreJni.toxFinalize(instanceNumber)
    } catch {
      case e: Throwable =>
        logger.error("Exception caught in finalizer; this indicates a serious problem in native code", e)
    }
    super.finalize()
  }

  @throws[ToxBootstrapException]
  override def bootstrap(address: String, port: Port, publicKey: ToxPublicKey): Unit = {
    ToxCoreImpl.checkBootstrapArguments(port.value, publicKey.value)
    ToxCoreJni.toxBootstrap(instanceNumber, address, port.value, publicKey.value)
  }

  @throws[ToxBootstrapException]
  override def addTcpRelay(address: String, port: Port, publicKey: ToxPublicKey): Unit = {
    ToxCoreImpl.checkBootstrapArguments(port.value, publicKey.value)
    ToxCoreJni.toxAddTcpRelay(instanceNumber, address, port.value, publicKey.value)
  }

  override def getSavedata: Array[Byte] =
    ToxCoreJni.toxGetSavedata(instanceNumber)

  @throws[ToxGetPortException]
  override def getUdpPort: Port =
    Port.unsafeFromInt(ToxCoreJni.toxSelfGetUdpPort(instanceNumber))

  @throws[ToxGetPortException]
  override def getTcpPort: Port =
    Port.unsafeFromInt(ToxCoreJni.toxSelfGetTcpPort(instanceNumber))

  override def getDhtId: ToxPublicKey =
    ToxPublicKey.unsafeFromValue(ToxCoreJni.toxSelfGetDhtId(instanceNumber))

  override def iterationInterval: Int =
    ToxCoreJni.toxIterationInterval(instanceNumber)

  override def iterate[S](@NotNull handler: ToxCoreEventListener[S])(state: S): S = {
    ToxCoreEventDispatch.dispatch(handler, ToxCoreJni.toxIterate(instanceNumber))(state)
  }

  override def getPublicKey: ToxPublicKey =
    ToxPublicKey.unsafeFromValue(ToxCoreJni.toxSelfGetPublicKey(instanceNumber))

  override def getSecretKey: ToxSecretKey =
    ToxSecretKey.unsafeFromValue(ToxCoreJni.toxSelfGetSecretKey(instanceNumber))

  override def setNospam(nospam: Int): Unit =
    ToxCoreJni.toxSelfSetNospam(instanceNumber, nospam)

  override def getNospam: Int =
    ToxCoreJni.toxSelfGetNospam(instanceNumber)

  override def getAddress: ToxFriendAddress =
    ToxFriendAddress.unsafeFromValue(ToxCoreJni.toxSelfGetAddress(instanceNumber))

  @throws[ToxSetInfoException]
  override def setName(name: ToxNickname): Unit = {
    ToxCoreImpl.checkInfoNotNull(name.value)
    ToxCoreJni.toxSelfSetName(instanceNumber, name.value)
  }

  override def getName: ToxNickname = {
    ToxNickname.unsafeFromValue(ToxCoreJni.toxSelfGetName(instanceNumber))
  }

  @throws[ToxSetInfoException]
  override def setStatusMessage(message: ToxStatusMessage): Unit = {
    ToxCoreImpl.checkInfoNotNull(message.value)
    ToxCoreJni.toxSelfSetStatusMessage(instanceNumber, message.value)
  }

  override def getStatusMessage: ToxStatusMessage =
    ToxStatusMessage.unsafeFromValue(ToxCoreJni.toxSelfGetStatusMessage(instanceNumber))

  override def setStatus(status: ToxUserStatus): Unit =
    ToxCoreJni.toxSelfSetStatus(instanceNumber, status.ordinal)

  override def getStatus: ToxUserStatus =
    ToxUserStatus.values()(ToxCoreJni.toxSelfGetStatus(instanceNumber))

  @throws[ToxFriendAddException]
  override def addFriend(address: ToxFriendAddress, message: ToxFriendRequestMessage): ToxFriendNumber = {
    ToxCoreImpl.checkLength("Friend Address", address.value, ToxCoreConstants.AddressSize)
    ToxFriendNumber.unsafeFromInt(ToxCoreJni.toxFriendAdd(instanceNumber, address.value, message.value))
  }

  @throws[ToxFriendAddException]
  override def addFriendNorequest(publicKey: ToxPublicKey): ToxFriendNumber = {
    ToxCoreImpl.checkLength("Public Key", publicKey.value, ToxCoreConstants.PublicKeySize)
    ToxFriendNumber.unsafeFromInt(ToxCoreJni.toxFriendAddNorequest(instanceNumber, publicKey.value))
  }

  @throws[ToxFriendDeleteException]
  override def deleteFriend(friendNumber: ToxFriendNumber): Unit =
    ToxCoreJni.toxFriendDelete(instanceNumber, friendNumber.value)

  @throws[ToxFriendByPublicKeyException]
  override def friendByPublicKey(publicKey: ToxPublicKey): ToxFriendNumber =
    ToxFriendNumber.unsafeFromInt(ToxCoreJni.toxFriendByPublicKey(instanceNumber, publicKey.value))

  @throws[ToxFriendGetPublicKeyException]
  override def getFriendPublicKey(friendNumber: ToxFriendNumber): ToxPublicKey =
    ToxPublicKey.unsafeFromValue(ToxCoreJni.toxFriendGetPublicKey(instanceNumber, friendNumber.value))

  override def friendExists(friendNumber: ToxFriendNumber): Boolean =
    ToxCoreJni.toxFriendExists(instanceNumber, friendNumber.value)

  override def getFriendList: Array[Int] =
    ToxCoreJni.toxSelfGetFriendList(instanceNumber)

  @throws[ToxSetTypingException]
  override def setTyping(friendNumber: ToxFriendNumber, typing: Boolean): Unit =
    ToxCoreJni.toxSelfSetTyping(instanceNumber, friendNumber.value, typing)

  @throws[ToxFriendSendMessageException]
  override def friendSendMessage(friendNumber: ToxFriendNumber, messageType: ToxMessageType, timeDelta: Int, message: ToxFriendMessage): Int =
    ToxCoreJni.toxFriendSendMessage(instanceNumber, friendNumber.value, messageType.ordinal, timeDelta, message.value)

  @throws[ToxFileControlException]
  override def fileControl(friendNumber: ToxFriendNumber, fileNumber: Int, control: ToxFileControl): Unit =
    ToxCoreJni.toxFileControl(instanceNumber, friendNumber.value, fileNumber, control.ordinal)

  @throws[ToxFileSeekException]
  override def fileSeek(friendNumber: ToxFriendNumber, fileNumber: Int, position: Long): Unit =
    ToxCoreJni.toxFileSeek(instanceNumber, friendNumber.value, fileNumber, position)

  @throws[ToxFileSendException]
  override def fileSend(friendNumber: ToxFriendNumber, kind: Int, fileSize: Long, @NotNull fileId: ToxFileId, filename: ToxFilename): Int =
    ToxCoreJni.toxFileSend(instanceNumber, friendNumber.value, kind, fileSize, fileId.value, filename.value)

  @throws[ToxFileSendChunkException]
  override def fileSendChunk(friendNumber: ToxFriendNumber, fileNumber: Int, position: Long, data: Array[Byte]): Unit =
    ToxCoreJni.toxFileSendChunk(instanceNumber, friendNumber.value, fileNumber, position, data)

  @throws[ToxFileGetException]
  override def getFileFileId(friendNumber: ToxFriendNumber, fileNumber: Int): ToxFileId =
    ToxFileId.unsafeFromValue(ToxCoreJni.toxFileGetFileId(instanceNumber, friendNumber.value, fileNumber))

  @throws[ToxFriendCustomPacketException]
  override def friendSendLossyPacket(friendNumber: ToxFriendNumber, data: ToxLossyPacket): Unit =
    ToxCoreJni.toxFriendSendLossyPacket(instanceNumber, friendNumber.value, data.value)

  @throws[ToxFriendCustomPacketException]
  override def friendSendLosslessPacket(friendNumber: ToxFriendNumber, data: ToxLosslessPacket): Unit =
    ToxCoreJni.toxFriendSendLosslessPacket(instanceNumber, friendNumber.value, data.value)

  def invokeFriendName(friendNumber: ToxFriendNumber, @NotNull name: ToxNickname): Unit =
    ToxCoreJni.invokeFriendName(instanceNumber, friendNumber.value, name.value)
  def invokeFriendStatusMessage(friendNumber: ToxFriendNumber, @NotNull message: Array[Byte]): Unit =
    ToxCoreJni.invokeFriendStatusMessage(instanceNumber, friendNumber.value, message)
  def invokeFriendStatus(friendNumber: ToxFriendNumber, @NotNull status: ToxUserStatus): Unit =
    ToxCoreJni.invokeFriendStatus(instanceNumber, friendNumber.value, status.ordinal())
  def invokeFriendConnectionStatus(friendNumber: ToxFriendNumber, @NotNull connectionStatus: ToxConnection): Unit =
    ToxCoreJni.invokeFriendConnectionStatus(instanceNumber, friendNumber.value, connectionStatus.ordinal())
  def invokeFriendTyping(friendNumber: ToxFriendNumber, isTyping: Boolean): Unit =
    ToxCoreJni.invokeFriendTyping(instanceNumber, friendNumber.value, isTyping)
  def invokeFriendReadReceipt(friendNumber: ToxFriendNumber, messageId: Int): Unit =
    ToxCoreJni.invokeFriendReadReceipt(instanceNumber, friendNumber.value, messageId)
  def invokeFriendRequest(@NotNull publicKey: ToxPublicKey, timeDelta: Int, @NotNull message: Array[Byte]): Unit =
    ToxCoreJni.invokeFriendRequest(instanceNumber, publicKey.value, timeDelta, message)
  def invokeFriendMessage(friendNumber: ToxFriendNumber, @NotNull messageType: ToxMessageType, timeDelta: Int, @NotNull message: Array[Byte]): Unit =
    ToxCoreJni.invokeFriendMessage(instanceNumber, friendNumber.value, messageType.ordinal(), timeDelta, message)
  def invokeFileChunkRequest(friendNumber: ToxFriendNumber, fileNumber: Int, position: Long, length: Int): Unit =
    ToxCoreJni.invokeFileChunkRequest(instanceNumber, friendNumber.value, fileNumber, position, length)
  def invokeFileRecv(friendNumber: ToxFriendNumber, fileNumber: Int, kind: Int, fileSize: Long, @NotNull filename: Array[Byte]): Unit =
    ToxCoreJni.invokeFileRecv(instanceNumber, friendNumber.value, fileNumber, kind, fileSize, filename)
  def invokeFileRecvChunk(friendNumber: ToxFriendNumber, fileNumber: Int, position: Long, @NotNull data: Array[Byte]): Unit =
    ToxCoreJni.invokeFileRecvChunk(instanceNumber, friendNumber.value, fileNumber, position, data)
  def invokeFileRecvControl(friendNumber: ToxFriendNumber, fileNumber: Int, @NotNull control: ToxFileControl): Unit =
    ToxCoreJni.invokeFileRecvControl(instanceNumber, friendNumber.value, fileNumber, control.ordinal())
  def invokeFriendLossyPacket(friendNumber: ToxFriendNumber, @NotNull data: Array[Byte]): Unit =
    ToxCoreJni.invokeFriendLossyPacket(instanceNumber, friendNumber.value, data)
  def invokeFriendLosslessPacket(friendNumber: ToxFriendNumber, @NotNull data: Array[Byte]): Unit =
    ToxCoreJni.invokeFriendLosslessPacket(instanceNumber, friendNumber.value, data)
  def invokeSelfConnectionStatus(@NotNull connectionStatus: ToxConnection): Unit =
    ToxCoreJni.invokeSelfConnectionStatus(instanceNumber, connectionStatus.ordinal())

}
