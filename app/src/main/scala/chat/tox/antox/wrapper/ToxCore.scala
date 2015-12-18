package chat.tox.antox.wrapper

import java.io.File

import chat.tox.antox.tox.{IntervalLevels, Intervals}
import chat.tox.antox.utils._
import im.tox.core.network.Port
import im.tox.tox4j.core.callbacks._
import im.tox.tox4j.core.data._
import im.tox.tox4j.core.enums._
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.impl.jni.{ToxCoreImpl, ToxCryptoImpl}

class ToxCore(groupList: GroupList, options: ToxOptions) extends Intervals {

  val tox = new ToxCoreImpl(options)

  var selfConnectionStatus: ToxConnection = ToxConnection.NONE

  def this(groupList: GroupList) {
    this(groupList, new ToxOptions)
  }

  def getTox: ToxCoreImpl = tox

  def close(): Unit = tox.close()

  def getSaveData: Array[Byte] = tox.getSavedata

  def bootstrap(address: String, port: Port, publicKey: ToxPublicKey): Unit = {
    tox.bootstrap(address, port, publicKey)
    tox.addTcpRelay(address, port, publicKey)
  }

  def getUdpPort: Port = tox.getUdpPort

  def getTcpPort: Port = tox.getTcpPort

  def getDhtId: ToxPublicKey = tox.getDhtId

  def iterationInterval(): Int = tox.iterationInterval

  def iterate(eventListener: ToxCoreEventListener[Unit]): Unit = tox.iterate[Unit](eventListener)(Unit)

  override def interval: Int = IntervalLevels.AWAKE.id

  def getSelfKey: SelfKey = new SelfKey(tox.getPublicKey.toHexString)

  def getSecretKey: ToxSecretKey = tox.getSecretKey

  def setNospam(nospam: Int): Unit = tox.setNospam(nospam)

  def getNospam: Int = tox.getNospam

  def getAddress: ToxAddress = new ToxAddress(tox.getAddress.value)

  def setName(name: ToxNickname): Unit = {
    tox.setName(name)
    for (groupNumber <- getGroupList) {
      try {
        //FIXME setGroupSelfName(groupNumber, name)
      } catch {
        case e: ToxException[_]  =>
          AntoxLog.debug("could not set name in group " + groupNumber)
      }
    }
  }

  def getName: ToxNickname = tox.getName

  def getSelfConnectionStatus: ToxConnection = selfConnectionStatus

  //should only ever be called from AntoxOnConnectionStatusCallback
  def setSelfConnectionStatus(selfConnectionStatus: ToxConnection): Unit = {
    this.selfConnectionStatus = selfConnectionStatus
  }

  def setStatusMessage(message: ToxStatusMessage): Unit = tox.setStatusMessage(message)

  def getStatusMessage: ToxStatusMessage = tox.getStatusMessage

  def setStatus(status: ToxUserStatus): Unit = tox.setStatus(status)

  def getStatus: ToxUserStatus = tox.getStatus

  def addFriend(address: ToxAddress, message: ToxFriendRequestMessage): ToxFriendNumber = {
    val friendNumber = tox.addFriend(ToxFriendAddress.unsafeFromValue(address.bytes), message)
   friendNumber
  }

  def addFriendNoRequest(key: ToxKey): ToxFriendNumber = {
    val friendNumber = tox.addFriendNorequest(ToxPublicKey.unsafeFromValue(key.bytes))
     friendNumber
  }

  def deleteFriend(friendKey: FriendKey): Unit = {
     tox.deleteFriend(getFriendNumber(friendKey))
  }

  def getFriendNumber(key: FriendKey): ToxFriendNumber = tox.friendByPublicKey(ToxPublicKey.unsafeFromValue(key.bytes))

  def getFriendKey(friendNumber: ToxFriendNumber): FriendKey = new FriendKey(tox.getFriendPublicKey(friendNumber).toHexString)

  def friendExists(friendKey: FriendKey): Boolean = tox.friendExists(getFriendNumber(friendKey))

  def getFriendList: Array[FriendKey] = tox.getFriendNumbers.map(getFriendKey)

  def setTyping(friendKey: FriendKey, typing: Boolean): Unit = tox.setTyping(getFriendNumber(friendKey), typing)

  def friendSendMessage(friendKey: FriendKey, message: ToxFriendMessage, messageType: ToxMessageType): Int =
    tox.friendSendMessage(getFriendNumber(friendKey), messageType, 0, message)

  def hash(bytes: Array[Byte]): Array[Byte] = ToxCryptoImpl.hash(bytes)

  def hash(file: File): Option[String] = {
    FileUtils.readToBytes(file).map(ToxCryptoImpl.hash).map(_.toString)
  }

  def fileControl(friendKey: FriendKey, fileNumber: Int, control: ToxFileControl): Unit = tox.fileControl(getFriendNumber(friendKey), fileNumber, control)

  def fileSend(friendKey: FriendKey, kind: Int, fileSize: Long, fileId: ToxFileId, filename: ToxFilename): Int = {
    tox.fileSend(getFriendNumber(friendKey), kind, fileSize, fileId, filename)
  }

  def fileSendChunk(friendKey: FriendKey, fileNumber: Int, position: Long, data: Array[Byte]): Unit =
    tox.fileSendChunk(getFriendNumber(friendKey), fileNumber, position, data)

  def fileGetFileId(friendKey: FriendKey, fileNumber: Int): ToxFileId = tox.getFileFileId(getFriendNumber(friendKey), fileNumber)

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

  def joinGroup(groupKey: ToxKey): Int = {
    //val groupNumber = tox.joinGroup(groupKey.bytes)
    //println("group number is " + groupNumber)
    //groupList.addGroup(this, groupNumber)
    //groupNumber
    0
  }

  def reconnectGroup(groupKey: GroupKey): Int = {
    //tox.reconnectGroup(groupNumber)
    0
  }

  def deleteGroup(groupKey: GroupKey, partMessage: String): Unit = {
    //tox.deleteGroup(groupNumber, partMessage.getBytes)
    groupList.removeGroup(groupKey)
  }

  def sendGroupMessage(groupKey: GroupKey, message: String): Unit = {} //tox.sendGroupMessage(groupNumber, message.getBytes)

  def sendGroupPrivateMessage(groupKey: GroupKey, peerNumber: Int, message: String): Unit = {} //tox.sendGroupPrivateMessage(groupNumber, peerNumber, message.getBytes)

  def sendGroupAction(groupKey: GroupKey, message: String): Unit = {} //tox.sendGroupAction(groupNumber, message.getBytes)

  def setGroupSelfName(groupKey: GroupKey, name: String): Unit = {} //tox.setGroupSelfName(groupNumber, name.getBytes)

  def setGroupSelfNameAll(name: String): Unit = {
    for (groupKey <- getGroupList) {
      var successful = true
      var attemptName = name
      do {
        try {
          setGroupSelfName(groupKey, attemptName)
        } catch {
          case e: ToxException[_] =>
            successful = false
            attemptName = name + "_"
        }
      } while (!successful && getGroupSelfName(groupKey).length < Constants.MAX_NAME_LENGTH)

      AntoxLog.debug("group name " + getGroupSelfName(groupKey))
    }
  }

  def getGroupPeerPublicKey(groupKey: GroupKey, peerNumber: Int): PeerKey = {
    new PeerKey("")
  }

  def getGroupPeerName(groupKey: GroupKey, peerNumber: Int): String = {
    //val peerNameBytes = tox.getGroupPeerName(groupNumber, peerNumber)
    //if (peerNameBytes == null) {
      ""
    //} else {
    //  new String(Array.empty[Byte], "UTF-8")
    //}
  }

  def getGroupSelfName(groupKey: GroupKey): String = "" //new String(tox.getGroupSelfName(groupNumber), "UTF-8")

  def setGroupTopic(groupKey: GroupKey, topic: Array[Byte]): Unit = {}//tox.setGroupTopic(groupNumber, topic)

  def getGroupTopic(groupKey: GroupKey): String = "" //new String(tox.getGroupTopic(groupNumber), "UTF-8")

  def getGroupName(groupKey: GroupKey): String = "" //new String(tox.getGroupName(groupNumber), "UTF-8")

  //def setGroupSelfStatus(groupKey: GroupKey, status: ToxGroupStatus): Unit = tox.setGroupSelfStatus(groupNumber, status)

  //def getGroupPeerStatus(groupKey: GroupKey, peerNumber: Int): ToxGroupStatus = tox.getGroupPeerStatus(groupNumber, peerNumber)

  //def getGroupPeerRole(groupKey: GroupKey, peerNumber: Int): ToxGroupRole = tox.getGroupPeerRole(groupNumber, peerNumber)

  def getGroupKey(groupNumber: Int): GroupKey =
    new GroupKey("") //(tox.getGroupChatId(groupNumber))

  def getGroupNumberPeers(groupKey: GroupKey): Int = 0 //tox.getGroupNumberPeers(groupNumber)

  def getGroupPeerlist(groupKey: GroupKey): Array[Int] = {
    /*if (tox.getGroupNumberPeers(groupNumber) == 0) {
      Array.empty[Int]
    } else {
      (0 until tox.getGroupNumberPeers(groupNumber)).toArray
    } */

    Array.empty[Int]
  }

  def getActiveGroupCount: Int = 0 // tox.getActiveGroupsCount

  def getGroupList: Array[GroupKey] = {
    /* if (tox.getActiveGroupsCount == 0) {
      Array.empty[Int]
    } else {
      (0 until tox.getActiveGroupsCount).toArray
    } */

    Array.empty[GroupKey]
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
