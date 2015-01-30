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

class ToxCore(antoxFriendList: AntoxFriendList, groupList: GroupList, options: ToxOptions, data: Array[Byte]) {

  val tox: ToxCoreImpl = new ToxCoreImpl(options, data)

  def this(antoxFriendList: AntoxFriendList, groupList: GroupList, data: Array[Byte]) {
    this(antoxFriendList: AntoxFriendList, groupList: GroupList, new ToxOptions, data)
  }

  def this(antoxFriendList: AntoxFriendList, groupList: GroupList, options: ToxOptions) {
    this(antoxFriendList: AntoxFriendList, groupList: GroupList, options, null)
  }

  def this(antoxFriendList: AntoxFriendList, groupList: GroupList) {
    this(antoxFriendList: AntoxFriendList, groupList: GroupList, new ToxOptions)
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

  /*
  ==========================
  GROUP FUNCTIONS START HERE
  ==========================
   */

  /** Adds a new groupchat to group chats array.
    *
    * @return groupNumber
    */
  def newGroup(name: String): Int = {
    println("Made a new group with name " + name)
    0
  }

  /** Joins a groupchat using the supplied public key.
    *
    * @return groupNumber
    */
  def joinGroup(inviteKey: String): Int = {
    println("Joined a group with key" + inviteKey)
    0
  }

  /** Joins a group using the invite data received in a friend's group invite.
    *
    * @return groupNumber on success.
    */
  def acceptGroupInvite(inviteData: Array[Byte]): Int = {
      println("Accepted a group invite.")
    0
  }

  /**
   * Invites friendnumber to groupNumber.
   */
  def inviteFriendToGroup(groupNumber: Int, friendNumber: Int): Boolean = {
    println("Invited friend " + friendNumber + " to group " + groupNumber)
    true
  }

  /**
   * Deletes groupNumber's group chat and sends an optional parting message to group peers
   * The maximum parting message length is TOX_MAX_GROUP_PART_LENGTH.
   */
  def deleteGroup(groupNumber: Int, partMessage: String): Unit = {
    println("Deleted group " + groupNumber)
  }

  /** Sends a groupchat message to groupnumber. Messages should be split at TOX_MAX_MESSAGE_LENGTH bytes.
    */
  def sendGroupMessage(groupNumber: Int, message: String): Unit =  {
    println("Sent group message " + message + " to group " + groupNumber)
  }

  /** Sends a private message to peernumber in groupnumber. Messages should be split at TOX_MAX_MESSAGE_LENGTH bytes.
    */
  def sendGroupPrivateMessage(groupNumber: Int, peerNumber: Int, message: String): Unit =  {
    println("Sent group private message " + message + " to peer " + peerNumber + " in group " + groupNumber)
  }

  /** Sends a groupchat action message to groupnumber. Messages should be split at TOX_MAX_MESSAGE_LENGTH bytes.
    */
  def sendGroupAction(groupNumber: Int, message: String): Unit =  {
    println("Sent group action " + message + " to group " + groupNumber)
  }

  /*/** Issues a groupchat operator certificate for peernumber to groupnumber.
    * type must be a TOX_GROUP_OP_CERTIFICATE.
    */
  def sendGroupOpCertificate(groupNumber: Int, peernumber: Int, certType: Int) {

  } */

  /** Sets your name for groupnumber. length should be no larger than TOX_MAX_NAME_LENGTH bytes.
    */
  def setGroupSelfName(groupNumber: Int, name: String) {
    println("Set name in group " + groupNumber + " to name " + name)
  }

  /** Get peernumber's name in groupnumber's group chat.
    */
  def getGroupPeerName(groupNumber: Int, peerNumber: Int): String = {
    println("Got group peer name in group " + groupNumber)
    "PEERNAME" + peerNumber
  }

  /** Get your own name for groupnumber's group.
    * name buffer must be at least TOX_MAX_NAME_LENGTH bytes.
    */
  def getGroupSelfName(groupNumber: Int): String = {
    println("Got name of group " + groupNumber)
    "GROUPNAME" + groupNumber
  }

  /**
    * Sets groupnumber's topic.
    */
  def setGroupTopic(groupNumber: Int, topic: String) {
    println("Set group " + groupNumber + "'s topic to " + topic)
  }

  /** Gets groupnumber's topic. topic buffer must be at least TOX_MAX_GROUP_TOPIC_LENGTH bytes.
    */
  def getGroupTopic(groupNumber: Int): String = {
    "TOPIC" + groupNumber
  }

  /** Gets groupnumber's group name. groupname buffer must be at least TOX_MAX_GROUP_NAME_LENGTH bytes.
    */
  def getGroupName(groupNumber: Int): String = {
    "NAME" + groupNumber
  }

  /** Sets your status for groupnumber.
    */
  def setSelfStatusInGroup(groupNumber: Int, status: String) = {
    println("Set group " + groupNumber + "'s status to " + status)
  }

  /** Get peernumber's status in groupnumber's group chat.
    *
    * @return a TOX_GROUP_STATUS on success.
    * @return TOX_GS_INVALID on failure.
    */
  def getGroupPeerStatus(groupNumber: Int, peernumber: Int): ToxStatus = {
    return ToxStatus.AWAY
  }

  /* /** Get peernumber's group role in groupnumber's group chat.
    *
    * @return a TOX_GROUP_ROLE on success.
    * @return TOX_GR_INVALID on failure.
    */
  def getPeerInGroupRole(groupNumber: Int, peernumber: Int) {

  } */

  /**
   * Get invite key for the groupchat from the groupnumber.
    */
  def getGroupInviteKey(groupNumber: Int): String = {
    "FB21BD88F0ECBBBA92EDE8BEFF35F627EB6B46FBF7021019933F209710526B4" +
    "81CC3BAB15720FDCD94A50D8EB897167FB850DF1E77EA23C3E34EED224161550D"
  }

    /**
    * @return the nicks of the peers in groupnumber
    */
  def getGroupNames(groupNumber: Int): Array[String] = {
    Array("Zetok", "subliun", "I-hate-gentoo", "downwithgroupbot")
  }

  /**
   * @return the number of peers in groupnumber.
   */
  def getGroupNumberPeers(groupNumber: Int): Int = {
    4
  }

  /** Toggle ignore on peernumber in groupnumber.
    * If ignore is true, group and private messages from peernumber are ignored, as well as A/V.
    * If ignore is false, peer is unignored.
    */
  def toggleGroupIgnorePeer(groupNumber: Int, peernumber: Int, ignore: Boolean): Unit = {

  }
}
