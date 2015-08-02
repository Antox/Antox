package chat.tox.antox.wrapper

import java.io.File

import chat.tox.antox.tox.{IntervalLevels, Intervals, ToxSingleton}
import chat.tox.antox.utils._
import im.tox.tox4j.core.callbacks._
import im.tox.tox4j.core.enums._
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.impl.jni.{ToxCoreImpl, ToxCryptoImpl}

class ToxCore(antoxFriendList: AntoxFriendList, groupList: GroupList, options: ToxOptions) extends Intervals {

  val tox = new ToxCoreImpl[Unit](options)

  var selfConnectionStatus: ToxConnection = ToxConnection.NONE

  def this(antoxFriendList: AntoxFriendList, groupList: GroupList) {
    this(antoxFriendList, groupList, new ToxOptions)
  }

  def getTox = tox

  def close(): Unit = tox.close()

  def getSaveData: Array[Byte] = tox.getSavedata

  def bootstrap(address: String, port: Int, publicKey: ToxKey): Unit = {
    tox.bootstrap(address, port, publicKey.bytes)
    tox.addTcpRelay(address, port, publicKey.bytes)
  }

  def getUdpPort: Int = tox.getUdpPort

  def getTcpPort: Int = tox.getTcpPort

  def getDhtId: Array[Byte] = tox.getDhtId

  def iterationInterval(): Int = tox.iterationInterval

  def iterate(): Unit = tox.iterate(Unit)

  override def interval: Int = IntervalLevels.AWAKE.id

  def getSelfKey: ToxKey = new ToxKey(tox.getPublicKey)

  def getSecretKey: Array[Byte] = tox.getSecretKey

  def setNospam(nospam: Int): Unit = tox.setNospam(nospam)

  def getNospam: Int = tox.getNospam

  def getAddress: ToxAddress = new ToxAddress(tox.getAddress)

  def setName(name: String): Unit = {
    tox.setName(name.getBytes)
    for (groupNumber <- getGroupList) {
      try {
        //FIXME setGroupSelfName(groupNumber, name)
      } catch {
        case e: ToxException[_]  =>
          println("could not set name in group " + groupNumber)
      }
    }
  }

  def getName: String = new String(tox.getName, "UTF-8")

  def getSelfConnectionStatus: ToxConnection = selfConnectionStatus

  //should only ever be called from AntoxOnConnectionStatusCallback
  def setSelfConnectionStatus(selfConnectionStatus: ToxConnection): Unit = {
    this.selfConnectionStatus = selfConnectionStatus
  }

  def setStatusMessage(message: String): Unit = tox.setStatusMessage(message.getBytes)

  def getStatusMessage: String = new String(tox.getStatusMessage, "UTF-8")

  def setStatus(status: ToxUserStatus): Unit = tox.setStatus(status)

  def getStatus: ToxUserStatus = tox.getStatus

  def addFriend(address: ToxAddress, message: String): Int = {
    val friendNumber = tox.addFriend(address.bytes, message.getBytes)
    antoxFriendList.addFriend(friendNumber)
    val antoxFriend = antoxFriendList.getByFriendNumber(friendNumber).get
    antoxFriend.setKey(address.key)
    friendNumber
  }

  def addFriendNoRequest(key: ToxKey): Int = {
    val friendNumber = tox.addFriendNorequest(Hex.hexStringToBytes(key.toString))
    antoxFriendList.addFriendIfNotExists(friendNumber)
    val antoxFriend = antoxFriendList.getByFriendNumber(friendNumber).get
    antoxFriend.setKey(key)
    friendNumber
  }

  def deleteFriend(friendNumber: Int): Unit = {
    ToxSingleton.getAntoxFriendList.removeFriend(friendNumber)
    tox.deleteFriend(friendNumber)
  }

  def getFriendByKey(key: ToxKey): Int = tox.friendByPublicKey(key.bytes)

  def getFriendKey(friendNumber: Int): ToxKey = new ToxKey(tox.getFriendPublicKey(friendNumber))

  def friendExists(friendNumber: Int): Boolean = tox.friendExists(friendNumber)

  def getFriendList: Array[Int] = tox.getFriendList

  def setTyping(friendNumber: Int, typing: Boolean): Unit = tox.setTyping(friendNumber, typing)

  def sendMessage(friendNumber: Int, message: String): Int = tox.friendSendMessage(friendNumber, ToxMessageType.NORMAL, 0, message.getBytes)

  def sendAction(friendNumber: Int, action: String): Int = tox.friendSendMessage(friendNumber, ToxMessageType.ACTION, 0, action.getBytes)

  def hash(bytes: Array[Byte]): Array[Byte] = ToxCryptoImpl.hash(bytes)

  def hash(file: File): Option[String] = {
    FileUtils.readToBytes(file).map(ToxCryptoImpl.hash).map(_.toString)
  }

  def fileControl(friendNumber: Int, fileNumber: Int, control: ToxFileControl): Unit = tox.fileControl(friendNumber, fileNumber, control)

  def fileSend(friendNumber: Int, kind: Int, fileSize: Long, fileId: String, filename: String): Int = {
    val fileIdBytes = Option(fileId).map(_.getBytes).orNull
    tox.fileSend(friendNumber, kind, fileSize, fileIdBytes, filename.getBytes)
  }

  def fileSendChunk(friendNumber: Int, fileNumber: Int, position: Long, data: Array[Byte]): Unit = tox.fileSendChunk(friendNumber, fileNumber, position, data)

  def fileGetFileId(friendNumber: Int, fileNumber: Int): Array[Byte] = Array[Byte](0) //tox.fileGetFileId(friendNumber, fileNumber)

  def callback(handler: ToxEventListener[Unit]): Unit = tox.callback(handler)

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
          case e: ToxException[_] =>
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

  def getGroupKey(groupNumber: Int): ToxKey =
    new ToxKey("") //(tox.getGroupChatId(groupNumber))

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
