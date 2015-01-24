package im.tox.antox.wrapper

;

import android.util.Log
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils._
import im.tox.tox4j.core.ToxOptions
import im.tox.tox4j.core.callbacks._
import im.tox.tox4j.core.enums.{ToxStatus, ToxFileControl, ToxFileKind}
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.{ToxAvImpl, ToxCoreImpl}

class ToxCore(antoxFriendList: AntoxFriendList, options: ToxOptions, data: Array[Byte]) {

  val tox: ToxCoreImpl = new ToxCoreImpl(options, data)

  def this(antoxFriendList: AntoxFriendList, data: Array[Byte]) {
    this(antoxFriendList: AntoxFriendList, new ToxOptions, data)
  }

  def this(antoxFriendList: AntoxFriendList, options: ToxOptions) {
    this(antoxFriendList: AntoxFriendList, options, null)
  }

  def this(antoxFriendList: AntoxFriendList) {
    this(antoxFriendList: AntoxFriendList, new ToxOptions)
  }

  def getTox = tox

  def close(): Unit = tox.close()

  override def finalize(): Unit = tox.finalize()

  def save(): Array[Byte] = tox.save()

  def bootstrap(p1: String, p2: Int, p3: String): Unit = {
    tox.bootstrap(p1, p2, Hex.hexStringToBytes(p3))
  }

  def callbackConnectionStatus(p1: ConnectionStatusCallback): Unit = tox.callbackConnectionStatus(p1)

  def getUdpPort: Int = tox.getUdpPort

  def getTcpPort: Int = tox.getTcpPort

  def getDhtId: Array[Byte] = tox.getDhtId

  def iterationInterval(): Int = tox.iterationInterval()

  def iteration(): Unit = tox.iteration()

  def getClientId: String = Hex.bytesToHexString(tox.getClientId)

  def getPrivateKey: Array[Byte] = tox.getPrivateKey

  def setNospam(nospam: Int): Unit = tox.setNospam(nospam)

  def getNospam: Int = tox.getNospam

  def getAddress: String = Hex.bytesToHexString(tox.getAddress)

  def setName(name: String): Unit = tox.setName(name.getBytes)

  def getName: String = new String(tox.getName, "UTF-8")

  def setStatusMessage(message: String): Unit = tox.setStatusMessage(message.getBytes)

  def getStatusMessage: String = new String(tox.getStatusMessage, "UTF-8")

  def setStatus(status: ToxStatus): Unit = tox.setStatus(status)

  def getStatus: ToxStatus = tox.getStatus

  def addFriend(address: String, message: String): Int = {
    val friendNumber = tox.addFriend(Hex.hexStringToBytes(address), message.getBytes)
    antoxFriendList.addFriend(friendNumber)
    val antoxFriend = antoxFriendList.getByFriendNumber(friendNumber).get
    antoxFriend.setClientId(ToxSingleton.clientIdFromAddress(address))
    return friendNumber
  }

  def addFriendNoRequest(clientId: String): Int = {
    val friendNumber = tox.addFriendNoRequest(Hex.hexStringToBytes(clientId))
    antoxFriendList.addFriend(friendNumber)
    val antoxFriend = antoxFriendList.getByFriendNumber(friendNumber).get
    antoxFriend.setClientId(clientId)
    return friendNumber
  }

  def deleteFriend(friendNumber: Int): Unit = {
    ToxSingleton.getAntoxFriendList.removeFriend(friendNumber)
    tox.deleteFriend(friendNumber)
  }

  def getFriendByClientId(clientId: String): Int = tox.getFriendByClientId(Hex.hexStringToBytes(clientId))

  def getClientId(friendNumber: Int): String = Hex.bytesToHexString(tox.getClientId(friendNumber))

  def friendExists(friendNumber: Int): Boolean = tox.friendExists(friendNumber)

  def getFriendList: Array[Int] = tox.getFriendList

  def setTyping(friendNumber: Int, typing: Boolean): Unit = tox.setTyping(friendNumber, typing)

  def sendMessage(friendNumber: Int, message: String): Int = tox.sendMessage(friendNumber, message.getBytes)

  def sendAction(friendNumber: Int, action: String): Int = tox.sendAction(friendNumber, action.getBytes)

  def callbackFriendName(callback: FriendNameCallback): Unit = tox.callbackFriendName(callback)

  def callbackFriendStatusMessage(callback: FriendStatusMessageCallback): Unit = tox.callbackFriendStatusMessage(callback)

  def callbackFriendStatus(callback: FriendStatusCallback): Unit = tox.callbackFriendStatus(callback)

  def callbackFriendConnected(callback: FriendConnectionStatusCallback): Unit = tox.callbackFriendConnected(callback)

  def callbackFriendTyping(callback: FriendTypingCallback): Unit = tox.callbackFriendTyping(callback)

  def callbackReadReceipt(callback: ReadReceiptCallback): Unit = tox.callbackReadReceipt(callback)

  def callbackFriendRequest(callback: FriendRequestCallback): Unit = tox.callbackFriendRequest(callback)

  def callbackFriendMessage(callback: FriendMessageCallback): Unit = tox.callbackFriendMessage(callback)

  def callbackFriendAction(callback: FriendActionCallback): Unit = tox.callbackFriendAction(callback)

  def fileControl(friendNumber: Int, fileNumber: Int, control: ToxFileControl): Unit = tox.fileControl(friendNumber, fileNumber, control)

  def callbackFileControl(callback: FileControlCallback): Unit = tox.callbackFileControl(callback)

  def fileSend(friendNumber: Int, fileNumber: ToxFileKind, fileSize: Long, filename: String): Int = tox.fileSend(friendNumber, fileNumber, fileSize, filename.getBytes)

  def fileSendChunk(friendNumber: Int, fileNumber: Int, data: Array[Byte]): Unit = tox.fileSendChunk(friendNumber, fileNumber, data)

  def callbackFileRequestChunk(callback: FileRequestChunkCallback): Unit = tox.callbackFileRequestChunk(callback)

  def callbackFileReceive(callback: FileReceiveCallback): Unit = tox.callbackFileReceive(callback)

  def callbackFileReceiveChunk(callback: FileReceiveChunkCallback): Unit = tox.callbackFileReceiveChunk(callback)

  def sendLossyPacket(friendNumber: Int, data: Array[Byte]): Unit = tox.sendLossyPacket(friendNumber, data)

  def callbackFriendLossyPacket(callback: FriendLossyPacketCallback): Unit = tox.callbackFriendLossyPacket(callback)

  def sendLosslessPacket(friendNumber: Int, data: Array[Byte]): Unit = tox.sendLosslessPacket(friendNumber, data)

  def callbackFriendLosslessPacket(callback: FriendLosslessPacketCallback): Unit = tox.callbackFriendLosslessPacket(callback)

  def callback(handler: ToxEventListener): Unit = tox.callback(handler)
}
