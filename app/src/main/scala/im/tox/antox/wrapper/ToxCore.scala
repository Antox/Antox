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

/*
  /** Adds a new groupchat to group chats array.
    *
    * @return groupNumber
    */
  def newGroup(name: String): Int = {
    println("Made a new group with name " + name)
    return 0
  }

  /** Joins a groupchat using the supplied public key.
    *
    * @return groupNumber
    */
  def joinGroup(invite_key: String): Int = {
    return 0
  }

  /** Joins a group using the invite data received in a friend's group invite.
    *
    * @return groupNumber on success.
    */
  def acceptGroupInvite(invite_data: Array[Byte]): Int = {
    return 0
  }

  /**
   * Invites friendnumber to groupNumber.
   */
  def inviteFriendToGroup(groupnumber: Int, friendnumber: Int): Boolean = {
    return true
  }

  /**
   * Deletes groupNumber's group chat and sends an optional parting message to group peers
   * The maximum parting message length is TOX_MAX_GROUP_PART_LENGTH.
   */
  def deleteGroup(groupNumber: Int, partMessage: String): Unit = {

  }

  /** Sends a groupchat message to groupnumber. Messages should be split at TOX_MAX_MESSAGE_LENGTH bytes.
    */
  def sendGroupMessage(groupNumber: Int, message: String): Unit =  {

  }

  /** Sends a private message to peernumber in groupnumber. Messages should be split at TOX_MAX_MESSAGE_LENGTH bytes.
    */
  def sendGroupPrivateMessage(groupNumber: Int, peernumber: Int, message: String): Unit =  {

  }

  /** Sends a groupchat action message to groupnumber. Messages should be split at TOX_MAX_MESSAGE_LENGTH bytes.
    */
  def sendGroupAction(groupNumber: Int, message: String): Unit =  {

  }

  /** Issues a groupchat operator certificate for peernumber to groupnumber.
    * type must be a TOX_GROUP_OP_CERTIFICATE.
    */
  def sendGroupOpCertificate(groupNumber: Int, peernumber: Int, certType: Int) {

  }

  /** Sets your name for groupnumber. length should be no larger than TOX_MAX_NAME_LENGTH bytes.
    */
  def setGroupSelfName(groupNumber: Int, name: String) {

  }

  /** Get peernumber's name in groupnumber's group chat.
    * name buffer must be at least TOX_MAX_NAME_LENGTH bytes.
    */
  def getGroupPeerName(groupNumber: Int, peernumber: Int, name: String) {

  }

  /** Get your own name for groupnumber's group.
    * name buffer must be at least TOX_MAX_NAME_LENGTH bytes.
    */
  def getGroupSelfName(groupNumber: Int
  {

  }

  /**
    * Sets groupnumber's topic.
    */
  def setGroupTopic(groupNumber: Int, topic: String)
  {

  }

  /** Gets groupnumber's topic. topic buffer must be at least TOX_MAX_GROUP_TOPIC_LENGTH bytes.
    */
  def tox_group_get_topic(groupNumber: Int, uint8_t

  * topic)
  {

  }

  /** Gets groupnumber's group name. groupname buffer must be at least TOX_MAX_GROUP_NAME_LENGTH bytes.
    */
  def getGroupName(groupNumber: Int, uint8_t

  * groupname)
  {

  }

  /** Sets your status for groupnumber.
    */
  def tox_group_set_status(groupNumber: Int, uint8_t

  status_type)
  {

  }

  /** Get peernumber's status in groupnumber's group chat.
    *
    * @return a TOX_GROUP_STATUS on success.
    * @return TOX_GS_INVALID on failure.
    */
  uint8_t tox_group_get_status (groupNumber: Int, uint32_t peernumber) {

  }

  /** Get peernumber's group role in groupnumber's group chat.
    *
    * @return a TOX_GROUP_ROLE on success.
    * @return TOX_GR_INVALID on failure.
    */
  uint8_t tox_group_get_role (groupNumber: Int, uint32_t peernumber) {

  }

  /** Get invite key for the groupchat from the groupnumber.
    * The result is stored in 'dest' which must have space for TOX_GROUP_CHAT_ID_SIZE bytes.
    */
  def tox_group_get_invite_key(groupNumber: Int) {

  }

  /** Copies the nicks of the peers in groupnumber to the nicks array.
    * Copies the lengths of the nicks to the lengths array.
    *
    * Arrays must have room for num_peers items.
    *
    * Should be used with tox_callback_group_peerlist_update.
    *
    * @return number of peers.
    */
  def tox_group_get_names(groupNumber: Int) {

  }

  /**
   * @return the number of peers in groupnumber.
   */
  def tox_group_get_number_peers(groupNumber: Int) {

  }

  /** Toggle ignore on peernumber in groupnumber.
    * If ignore is true, group and private messages from peernumber are ignored, as well as A/V.
    * If ignore is false, peer is unignored.
    */
  def tox_group_toggle_ignore(groupnumber: Int, peernumber: Int, ignore: Boolean) {

  } */
}
