package im.tox.antox.wrapper

import android.util.Log
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils._
import im.tox.tox4j.core.ToxOptions
import im.tox.tox4j.core.callbacks._
import im.tox.tox4j.core.enums._
import im.tox.tox4j.core.exceptions.ToxGroupSetSelfNameException
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.{ToxAvImpl, ToxCoreImpl}

class ToxCore(antoxFriendList: AntoxFriendList, groupList: GroupList, options: ToxOptions, data: Array[Byte]) {

  val tox: ToxCoreImpl = if (data != null) new ToxCoreImpl(options, data) else new ToxCoreImpl(options)

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

  def getSelfKey: String = Hex.bytesToHexString(tox.getPublicKey())

  def getSecretKey: Array[Byte] = tox.getSecretKey

  def setNospam(nospam: Int): Unit = tox.setNospam(nospam)

  def getNospam: Int = tox.getNospam

  def getAddress: String = Hex.bytesToHexString(tox.getAddress)

  def setName(name: String): Unit = {
    tox.setName(name.getBytes)
    for (groupNumber <- getGroupList) {
      try {
        //FIXME setGroupSelfName(groupNumber, name)
      } catch {
        case e: ToxException =>
          println("could not set name in group " + groupNumber)
      }
    }
    println("called set name")
  }

  def getName: String = new String(tox.getName, "UTF-8")

  def setStatusMessage(message: String): Unit = tox.setStatusMessage(message.getBytes)

  def getStatusMessage: String = new String(tox.getStatusMessage, "UTF-8")

  def setStatus(status: ToxStatus): Unit = tox.setStatus(status)

  def getStatus: ToxStatus = tox.getStatus

  def addFriend(address: String, message: String): Int = {
    val friendNumber = tox.addFriend(Hex.hexStringToBytes(address), message.getBytes)
    antoxFriendList.addFriend(friendNumber)
    val antoxFriend = antoxFriendList.getByFriendNumber(friendNumber).get
    antoxFriend.setKey(ToxSingleton.keyFromAddress(address))
    return friendNumber
  }

  def addFriendNoRequest(key: String): Int = {
    val friendNumber = tox.addFriendNoRequest(Hex.hexStringToBytes(key))
    antoxFriendList.addFriendIfNotExists(friendNumber)
    val antoxFriend = antoxFriendList.getByFriendNumber(friendNumber).get
    antoxFriend.setKey(key)
    return friendNumber
  }

  def deleteFriend(friendNumber: Int): Unit = {
    ToxSingleton.getAntoxFriendList.removeFriend(friendNumber)
    tox.deleteFriend(friendNumber)
  }

  def getFriendByKey(key: String): Int = tox.getFriendByPublicKey(Hex.hexStringToBytes(key))

  def getFriendKey(friendNumber: Int): String = Hex.bytesToHexString(tox.getPublicKey(friendNumber))

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

  def callbackGroupJoinRejected(callback: GroupJoinRejectedCallback): Unit = tox.callbackGroupJoinRejected(callback)

  def acceptGroupInvite(inviteData: Array[Byte]): Int = {
    val groupNumber = tox.acceptGroupInvite(inviteData)
    println("group invited with " + groupNumber + " and id ")
    groupList.addGroup(this, groupNumber)
    groupNumber
  }

  def newGroup(groupName: String): Int = {
    val groupNumber = tox.newGroup(groupName.getBytes)
    println("group created with " + groupNumber + " and id " + Hex.bytesToHexString(tox.getGroupChatId(groupNumber)))
    groupList.addGroup(this, groupNumber)
    groupList.getGroup(groupNumber).name = groupName
    groupNumber
  }

  def joinGroup(groupId: String): Int = {
    val groupNumber = tox.joinGroup(Hex.hexStringToBytes(groupId))
    println("group number is " + groupNumber)
    groupList.addGroup(this, groupNumber)
    groupNumber
  }

  def reconnectGroup(groupNumber: Int): Int = {
    tox.reconnectGroup(groupNumber)
  }

  def deleteGroup(groupNumber: Int, partMessage: String): Unit = {
    tox.deleteGroup(groupNumber, partMessage.getBytes)
    groupList.removeGroup(groupNumber)
  }

  def sendGroupMessage(groupNumber: Int, message: String): Unit = tox.sendGroupMessage(groupNumber, message.getBytes)

  def sendGroupPrivateMessage(groupNumber: Int, peerNumber: Int, message: String): Unit = tox.sendGroupPrivateMessage(groupNumber, peerNumber, message.getBytes)

  def sendGroupAction(groupNumber: Int, message: String): Unit = tox.sendGroupAction(groupNumber, message.getBytes)

  def setGroupSelfName(groupNumber: Int, name: String): Unit = tox.setGroupSelfName(groupNumber, name.getBytes)

  def setGroupSelfNameAll(name: String): Unit = {
    for (groupNumber <- getGroupList) {
      var successful = false
      var attemptName = name
      while (!successful && getGroupSelfName(groupNumber).length < Constants.MAX_NAME_LENGTH) {
        successful = true
        try {
          setGroupSelfName(groupNumber, attemptName)
        } catch {
          case e: ToxGroupSetSelfNameException =>
            successful = false
            attemptName = name + "_"
        }
      }
      println("group name " + getGroupSelfName(groupNumber))
    }
  }

  def getGroupPeerName(groupNumber: Int, peerNumber: Int):String = {
    val peerNameBytes = tox.getGroupPeerName(groupNumber, peerNumber)
    if (peerNameBytes == null) {
      ""
    } else {
      new String(peerNameBytes, "UTF-8")
    }
  }

  def getGroupSelfName(groupNumber: Int): String = "" //new String(tox.getGroupSelfName(groupNumber), "UTF-8")

  def setGroupTopic(groupNumber: Int, topic: Array[Byte]): Unit = {}//tox.setGroupTopic(groupNumber, topic)

  def getGroupTopic(groupNumber: Int): String = "" //new String(tox.getGroupTopic(groupNumber), "UTF-8")

  def getGroupName(groupNumber: Int): String = "" //new String(tox.getGroupName(groupNumber), "UTF-8")

  //def setGroupSelfStatus(groupNumber: Int, status: ToxGroupStatus): Unit = tox.setGroupSelfStatus(groupNumber, status)

  //def getGroupPeerStatus(groupNumber: Int, peerNumber: Int): ToxGroupStatus = tox.getGroupPeerStatus(groupNumber, peerNumber)

  //def getGroupPeerRole(groupNumber: Int, peerNumber: Int): ToxGroupRole = tox.getGroupPeerRole(groupNumber, peerNumber)

  def getGroupChatId(groupNumber: Int): String = "" //Hex.bytesToHexString(tox.getGroupChatId(groupNumber))

  def getGroupNumberPeers(groupNumber: Int): Int = 0 //tox.getGroupNumberPeers(groupNumber)

  def getGroupPeerlist(groupNumber: Int): Array[Int] = {
    /*if (tox.getGroupNumberPeers(groupNumber) == 0) {
      Array.empty[Int]
    } else {
      (0 until tox.getGroupNumberPeers(groupNumber)).toArray
    } */

    Array.empty[Int]
  }

  def getActiveGroupCount: Int = 0 // tox.getActiveGroupsCount

  def getGroupList: Array[Int] = {
    /* if (tox.getActiveGroupsCount == 0) {
      Array.empty[Int]
    } else {
      (0 until tox.getActiveGroupsCount).toArray
    } */

    Array.empty[Int]
  }

  /* def callbackGroupInvite(callback: GroupInviteCallback): Unit = tox.callbackGroupInvite(callback)

  def callbackGroupMessage(callback: GroupMessageCallback): Unit = tox.callbackGroupMessage(callback)

  def callbackGroupPrivateMessage(callback: GroupPrivateMessageCallback): Unit = tox.callbackGroupPrivateMessage(callback)

  def callbackGroupAction(callback: GroupActionCallback): Unit = tox.callbackGroupAction(callback)

  def callbackGroupNickChange(callback: GroupNickChangeCallback): Unit = tox.callbackGroupNickChange(callback)

  def callbackGroupTopicChange(callback: GroupTopicChangeCallback): Unit = tox.callbackGroupTopicChange(callback)

  def callbackPeerJoin(callback: GroupPeerJoinCallback): Unit = tox.callbackPeerJoin(callback)

  def callbackPeerExit(callback: GroupPeerExitCallback): Unit = tox.callbackPeerExit(callback)

  def callbackGroupSelfJoin(callback: GroupSelfJoinCallback): Unit = tox.callbackGroupSelfJoin(callback)

  def callbackGroupPeerlistUpdate(callback: GroupPeerlistUpdateCallback): Unit = tox.callbackGroupPeerlistUpdate(callback)

  def callbackGroupSelfTimeout(callback: GroupSelfTimeoutCallback): Unit = tox.callbackGroupSelfTimeout(callback) */

}
