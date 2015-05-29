package im.tox.antox.wrapper

import java.io.File

import im.tox.antox.tox.ToxSingleton
import im.tox.antox.transfer.FileUtils
import im.tox.antox.utils._
import im.tox.tox4j.ToxCoreImpl
import im.tox.tox4j.core.ToxOptions
import im.tox.tox4j.core.callbacks._
import im.tox.tox4j.core.enums._
import im.tox.tox4j.exceptions.ToxException

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

  def bootstrap(address: String, port: Int, publicKey: String): Unit = {
    tox.bootstrap(address, port, Hex.hexStringToBytes(publicKey))
    tox.addTcpRelay(address, port, Hex.hexStringToBytes(publicKey))
  }

  def callbackConnectionStatus(p1: ConnectionStatusCallback): Unit = tox.callbackConnectionStatus(p1)

  def getUdpPort: Int = tox.getUdpPort

  def getTcpPort: Int = tox.getTcpPort

  def getDhtId: Array[Byte] = tox.getDhtId

  def iterationInterval(): Int = tox.iterationInterval()

  def iteration(): Unit = tox.iteration()

  def getSelfKey: String = Hex.bytesToHexString(tox.getPublicKey)

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
    friendNumber
  }

  def addFriendNoRequest(key: String): Int = {
    val friendNumber = tox.addFriendNoRequest(Hex.hexStringToBytes(key))
    antoxFriendList.addFriendIfNotExists(friendNumber)
    val antoxFriend = antoxFriendList.getByFriendNumber(friendNumber).get
    antoxFriend.setKey(key)
    friendNumber
  }

  def deleteFriend(friendNumber: Int): Unit = {
    ToxSingleton.getAntoxFriendList.removeFriend(friendNumber)
    tox.deleteFriend(friendNumber)
  }

  def getFriendByKey(key: String): Int = tox.getFriendByPublicKey(Hex.hexStringToBytes(key))

  def getFriendKey(friendNumber: Int): String = Hex.bytesToHexString(tox.getFriendPublicKey(friendNumber))

  def friendExists(friendNumber: Int): Boolean = tox.friendExists(friendNumber)

  def getFriendList: Array[Int] = tox.getFriendList

  def setTyping(friendNumber: Int, typing: Boolean): Unit = tox.setTyping(friendNumber, typing)

  def sendMessage(friendNumber: Int, message: String): Int = tox.sendMessage(friendNumber, ToxMessageType.NORMAL, 0, message.getBytes)

  def sendAction(friendNumber: Int, action: String): Int = tox.sendMessage(friendNumber, ToxMessageType.ACTION, 0, action.getBytes)

  def callbackFriendName(callback: FriendNameCallback): Unit = tox.callbackFriendName(callback)

  def callbackFriendStatusMessage(callback: FriendStatusMessageCallback): Unit = tox.callbackFriendStatusMessage(callback)

  def callbackFriendStatus(callback: FriendStatusCallback): Unit = tox.callbackFriendStatus(callback)

  def callbackFriendConnected(callback: FriendConnectionStatusCallback): Unit = tox.callbackFriendConnected(callback)

  def callbackFriendTyping(callback: FriendTypingCallback): Unit = tox.callbackFriendTyping(callback)

  def callbackReadReceipt(callback: ReadReceiptCallback): Unit = tox.callbackReadReceipt(callback)

  def callbackFriendRequest(callback: FriendRequestCallback): Unit = tox.callbackFriendRequest(callback)

  def callbackFriendMessage(callback: FriendMessageCallback): Unit = tox.callbackFriendMessage(callback)

  def hash(bytes: Array[Byte]): Array[Byte] = tox.hash(bytes)

  def hash(file: File): Option[String] = {
    FileUtils.readToBytes(file).map(tox.hash).map(_.toString)
  }

  def fileControl(friendNumber: Int, fileNumber: Int, control: ToxFileControl): Unit = tox.fileControl(friendNumber, fileNumber, control)

  def callbackFileControl(callback: FileControlCallback): Unit = tox.callbackFileControl(callback)

  def fileSend(friendNumber: Int, kind: Int, fileSize: Long, fileId: String, filename: String): Int = {
    val fileIdBytes = Option(fileId).map(_.getBytes).orNull
    tox.fileSend(friendNumber, kind, fileSize, fileIdBytes, filename.getBytes)
  }

  def fileSendChunk(friendNumber: Int, fileNumber: Int, position: Long, data: Array[Byte]): Unit = tox.fileSendChunk(friendNumber, fileNumber, position, data)

  def fileGetFileId(friendNumber: Int, fileNumber: Int): Array[Byte] = new Array[Byte](0) //tox.fileGetFileId(friendNumber, fileNumber)

  def callbackFileRequestChunk(callback: FileRequestChunkCallback): Unit = tox.callbackFileRequestChunk(callback)

  def callbackFileReceive(callback: FileReceiveCallback): Unit = tox.callbackFileReceive(callback)

  def callbackFileReceiveChunk(callback: FileReceiveChunkCallback): Unit = tox.callbackFileReceiveChunk(callback)

  def sendLossyPacket(friendNumber: Int, data: Array[Byte]): Unit = tox.sendLossyPacket(friendNumber, data)

  def callbackFriendLossyPacket(callback: FriendLossyPacketCallback): Unit = tox.callbackFriendLossyPacket(callback)

  def sendLosslessPacket(friendNumber: Int, data: Array[Byte]): Unit = tox.sendLosslessPacket(friendNumber, data)

  def callbackFriendLosslessPacket(callback: FriendLosslessPacketCallback): Unit = tox.callbackFriendLosslessPacket(callback)

  def callback(handler: ToxEventListener): Unit = tox.callback(handler)

  //def callbackGroupJoinRejected(callback: GroupJoinRejectedCallback): Unit = tox.callbackGroupJoinRejected(callback)

  def acceptGroupInvite(inviteData: Array[Byte]): Int = {
    //val groupNumber = tox.acceptGroupInvite(inviteData)
    //println("group invited with " + groupNumber + " and id ")
    //groupList.addGroup(this, groupNumber)
    //groupNumber
    0
  }

  def newGroup(groupName: String): Int = {
    //val groupNumber = tox.newGroup(groupName.getBytes)
    //println("group created with " + groupNumber + " and id " + Hex.bytesToHexString(tox.getGroupChatId(groupNumber)))
    //groupList.addGroup(this, groupNumber)
    //groupList.getGroup(groupNumber).name = groupName
    //groupNumber
    0
  }

  def joinGroup(groupId: String): Int = {
    //val groupNumber = tox.joinGroup(Hex.hexStringToBytes(groupId))
    //println("group number is " + groupNumber)
    //groupList.addGroup(this, groupNumber)
    //groupNumber
    0
  }

  def reconnectGroup(groupNumber: Int): Int = {
    //tox.reconnectGroup(groupNumber)
    0
  }

  def deleteGroup(groupNumber: Int, partMessage: String): Unit = {
    //tox.deleteGroup(groupNumber, partMessage.getBytes)
    groupList.removeGroup(groupNumber)
  }

  def sendGroupMessage(groupNumber: Int, message: String): Unit = {} //tox.sendGroupMessage(groupNumber, message.getBytes)

  def sendGroupPrivateMessage(groupNumber: Int, peerNumber: Int, message: String): Unit = {} //tox.sendGroupPrivateMessage(groupNumber, peerNumber, message.getBytes)

  def sendGroupAction(groupNumber: Int, message: String): Unit = {} //tox.sendGroupAction(groupNumber, message.getBytes)

  def setGroupSelfName(groupNumber: Int, name: String): Unit = {} //tox.setGroupSelfName(groupNumber, name.getBytes)

  def setGroupSelfNameAll(name: String): Unit = {
    for (groupNumber <- getGroupList) {
      var successful = false
      var attemptName = name
      while (!successful && getGroupSelfName(groupNumber).length < Constants.MAX_NAME_LENGTH) {
        successful = true
        try {
          setGroupSelfName(groupNumber, attemptName)
        } catch {
          case e: ToxException =>
            successful = false
            attemptName = name + "_"
        }
      }
      println("group name " + getGroupSelfName(groupNumber))
    }
  }

  def getGroupPeerName(groupNumber: Int, peerNumber: Int): String = {
    //val peerNameBytes = tox.getGroupPeerName(groupNumber, peerNumber)
    //if (peerNameBytes == null) {
      ""
    //} else {
    //  new String(Array.empty[Byte], "UTF-8")
    //}
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
