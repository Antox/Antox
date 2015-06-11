package im.tox.antox.tox

import java.io.{BufferedReader, File, InputStreamReader, Reader}
import java.net.URL
import java.nio.charset.Charset
import java.util

import android.app.NotificationManager
import android.content.{Context, SharedPreferences}
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.util.Log
import im.tox.antox.callbacks._
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.utils._
import im.tox.antox.wrapper._
import im.tox.tox4j.core.ToxOptions
import im.tox.tox4j.core.enums.ToxStatus
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.impl.ToxAvJni
import org.json.JSONObject
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

object ToxSingleton {

  private val TAG = "im.tox.antox.tox.ToxSingleton"

  def getInstance() = this

  var tox: ToxCore = _

  var toxAv: ToxAvJni = _

  private var antoxFriendList: AntoxFriendList = _

  private var groupList: GroupList = _

  var mNotificationManager: NotificationManager = _

  var dataFile: ToxDataFile = _

  var qrFile: File = _

  var typingMap: util.HashMap[String, Boolean] = new util.HashMap[String, Boolean]()

  var isInited: Boolean = false

  var activeKey: String = _

  var chatActive: Boolean = _

  var dhtNodes: Array[DhtNode] = Array()

  def interval: Int = {
    Math.min(State.transfers.interval, tox.interval)
  }

  def getAntoxFriendList: AntoxFriendList = antoxFriendList

  def getAntoxFriend(key: String): Option[Friend] = {
    try {
      antoxFriendList.getByKey(key)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        None
      }
    }
  }

  def getAntoxFriend(friendNumber: Int): Option[Friend] = {
    try {
      antoxFriendList.getByFriendNumber(friendNumber)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        None
      }
    }
  }

  def getGroupList: GroupList = groupList

  def getGroup(groupNumber: Int): Group = getGroupList.getGroup(groupNumber)

  def getGroup(groupId: String): Group = getGroupList.getGroup(groupId)

  def getGroupPeer(groupNumber: Int, peerNumber: Int): GroupPeer = getGroupList.getPeer(groupNumber, peerNumber)

  def keyFromAddress(address: String): String = {
    address.substring(0, 64) //Cut to the length of the public key portion of a tox address. TODO: make a class that represents the full tox address
  }

  def changeActiveKey(key: String) {
    Reactive.activeKey.onNext(Some(key))
  }

  def clearActiveKey() {
    Reactive.activeKey.onNext(None)
  }

  def exportDataFile(dest: File): Unit = {
    dataFile.exportFile(dest)
    ToxSingleton.save()
  }

  def clearUselessNotifications(key: String) {
    if (key != null && key != "") {
      val mFriend = getAntoxFriend(key)
      mFriend.foreach(friend => {
        try {
          if (mNotificationManager != null) mNotificationManager.cancel(friend.getFriendNumber)
        } catch {
          case e: Exception => e.printStackTrace()
        }
      })
    }
  }

  def updateContactsList(ctx: Context): Unit = {
    updateFriendsList(ctx)
    updateGroupList(ctx)
  }

  def updateFriendsList(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val friendList = antoxDB.getFriendList
      antoxDB.close()
      Reactive.friendList.onNext(friendList)
    } catch {
      case e: Exception => Reactive.friendList.onError(e)
    }
  }

  def updateGroupList(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val groupList = antoxDB.getGroupList
      antoxDB.close()
      Reactive.groupList.onNext(groupList)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def updateFriendRequests(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val friendRequest = antoxDB.getFriendRequestsList
      antoxDB.close()
      Reactive.friendRequests.onNext(friendRequest.toArray(new Array[FriendRequest](friendRequest.size)))
    } catch {
      case e: Exception => Reactive.friendRequests.onError(e)
    }
  }

  def updateGroupInvites(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val groupInvites = antoxDB.getGroupInvitesList
      antoxDB.close()
      Reactive.groupInvites.onNext(groupInvites.toArray(new Array[GroupInvite](groupInvites.size)))
    } catch {
      case e: Exception => Reactive.groupInvites.onError(e)
    }
  }

  def updateMessages(ctx: Context) {
    Reactive.updatedMessages.onNext(true)
    updateLastMessageMap(ctx)
    updateUnreadCountMap(ctx)
  }

  def updateLastMessageMap(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val map = antoxDB.getLastMessages
      antoxDB.close()
      Reactive.lastMessages.onNext(map)
    } catch {
      case e: Exception => Reactive.lastMessages.onError(e)
    }
  }

  def updateUnreadCountMap(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val map = antoxDB.getUnreadCounts
      antoxDB.close()
      Reactive.unreadCounts.onNext(map)
    } catch {
      case e: Exception => Reactive.unreadCounts.onError(e)
    }
  }

  def updateDhtNodes(ctx: Context) {
    Log.d(TAG, "updateDhtNodes")
    val connMgr = ctx.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connMgr.getActiveNetworkInfo
    if (networkInfo != null && networkInfo.isConnected) {
      Log.d(TAG, "updateDhtNodes: connected")
      Observable[JSONObject](subscriber => {
        Log.d(TAG, "updateDhtNodes: in observable")
         object JsonReader {

          private def readAll(rd: Reader): String = {
            val sb = new StringBuilder()
            var cp: Int = rd.read()
            while (cp != -1) {
              sb.append(cp.toChar)
              cp = rd.read()
            }
            sb.toString()
          }

          def readJsonFromUrl(url: String): JSONObject = {
            val is = new URL(url).openStream()
            try {
              val rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))
              val jsonText = readAll(rd)
              val json = new JSONObject(jsonText)
              json
            } catch {
              case e: Exception => {
                Log.e(TAG, "JsonReader readJsonFromUrl error: " + e)
                new JSONObject()
              }
              } finally {
                is.close()
              }
          }
        }
        try {
          Log.d(TAG, "updateDhtNodes: about to readJsonFromUrl")
          val json = JsonReader.readJsonFromUrl("https://dist-build.tox.im/Nodefile.json")
            subscriber.onNext(json)
            subscriber.onCompleted()
          } catch {
            case e: Exception => {
              Log.e(TAG, "update dht nodes error: " + e)
              subscriber.onError(e)
            }
          }
          }).map(json => {
            Log.d(TAG, json.toString)
            var dhtNodes: Array[DhtNode] = Array()
            val serverArray = json.getJSONArray("servers")
            for (i <- 0 until serverArray.length) {
              val jsonObject = serverArray.getJSONObject(i)
              dhtNodes +:= new DhtNode(
                jsonObject.getString("owner"),
                jsonObject.getString("ipv6"),
                jsonObject.getString("ipv4"),
                jsonObject.getString("pubkey"),
                jsonObject.getInt("port"))
            }
            dhtNodes
          }).subscribeOn(IOScheduler())
            .observeOn(AndroidMainThreadScheduler())
            .subscribe(nodes => {
              dhtNodes = nodes
              Log.d(TAG, "Trying to bootstrap")
              try {
                for (i <- 0 until nodes.size) {
                  tox.bootstrap(nodes(i).ipv4, nodes(i).port, nodes(i).key)
                }
              } catch {
                case e: Exception =>
              }
              Log.d(TAG, "Successfully bootstrapped")
              }, error => {
                Log.e(TAG, "Failed bootstrapping " + error)
              })
    }
  }

  def isToxConnected(preferences: SharedPreferences, context: Context): Boolean = {
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val wifiOnly = preferences.getBoolean("wifi_only", true)
    val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

    !(wifiOnly && !mWifi.isConnected)
  }

  def populateAntoxLists(db: AntoxDB): Unit = {
    for (i <- tox.getFriendList) {
      //this doesn't set the name, status message, status
      //or online status of the friend because they are now set during callbacks
      antoxFriendList.addFriendIfNotExists(i)
      antoxFriendList.getByFriendNumber(i).get.setKey(tox.getFriendKey(i))
    }

    for (i <- tox.getGroupList) {
      val groupId = tox.getGroupChatId(i)
      val details = db.getGroupDetails(groupId)
      groupList.addGroupIfNotExists(new Group(groupId, i, details._1, details._2, details._3, new PeerList()))
    }
  }

  def initTox(ctx: Context) {
    State.db = new AntoxDB(ctx).open(writeable = true)
    antoxFriendList = new AntoxFriendList()
    groupList = new GroupList()
    qrFile = ctx.getFileStreamPath("userkey_qr.png")
    dataFile = new ToxDataFile(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val udpEnabled = preferences.getBoolean("enable_udp", false)
    val options = new ToxOptions(udpEnabled, Options.ipv6Enabled)

    if (!dataFile.doesFileExist()) {
      try {
        tox = new ToxCore(antoxFriendList, groupList, options)
        dataFile.saveFile(tox.save())
        val editor = preferences.edit()
        editor.putString("tox_id", tox.getAddress)
        editor.commit()
      } catch {
        case e: ToxException[_] => e.printStackTrace()
      }
      } else {
        try {
          tox = new ToxCore(antoxFriendList, groupList, options, dataFile.loadFile())
          val editor = preferences.edit()
          editor.putString("tox_id", tox.getAddress)
          editor.commit()
        } catch {
          case e: ToxException[_] => e.printStackTrace()
        }
      }

      //toxAv = new ToxAvImpl(tox.getTox)

      val db = new AntoxDB(ctx).open(writeable = true)
      db.setAllOffline()

      val friends = db.getFriendList
      val groups = db.getGroupList

      for (friendNumber <- tox.getFriendList) {
        val friendKey = tox.getFriendKey(friendNumber)
        if (!db.doesFriendExist(friendKey)) {
          db.addFriend(friendKey, "", "", "")
        }
      }

      for (groupNumber <- tox.getGroupList) {
        val groupId = tox.getGroupChatId(groupNumber)
        if (!db.doesGroupExist(groupId)) {
          db.addGroup(groupId, "", "")
        }
      }

      if (friends.size > 0 || groups.size > 0) {
        populateAntoxLists(db)

        for (friend <- friends) {
          try {
            antoxFriendList.updateFromFriend(friend)
          } catch {
            case e: Exception =>
              try {
              tox.addFriendNoRequest(friend.key)
              } catch {
                case e: Exception =>
                  Log.d("ToxSingleton", "this should not happen (error adding friend on init)")
              }
          }
        }
      }

      updateGroupList(ctx)
      registerCallbacks(ctx)

      try {
        tox.setName(preferences.getString("nickname", ""))
        tox.setStatusMessage(preferences.getString("status_message", ""))
        var newStatus: ToxStatus = ToxStatus.NONE
        val newStatusString = preferences.getString("status", "")
        newStatus = UserStatus.getToxUserStatusFromString(newStatusString)
        tox.setStatus(newStatus)
      } catch {
        case e: ToxException[_] =>
      } finally {
        db.close()
      }
      updateDhtNodes(ctx)
    }


  def registerCallbacks(ctx: Context): Unit = {
    tox.callbackFriendMessage(new AntoxOnMessageCallback(ctx))
    tox.callbackFriendRequest(new AntoxOnFriendRequestCallback(ctx))
    tox.callbackFriendConnected(new AntoxOnConnectionStatusCallback(ctx))
    tox.callbackFriendName(new AntoxOnNameChangeCallback(ctx))
    tox.callbackReadReceipt(new AntoxOnReadReceiptCallback(ctx))
    tox.callbackFriendStatusMessage(new AntoxOnStatusMessageCallback(ctx))
    tox.callbackFriendStatus(new AntoxOnUserStatusCallback(ctx))
    tox.callbackFriendTyping(new AntoxOnTypingChangeCallback(ctx))
    tox.callbackFileReceive(new AntoxOnFileReceiveCallback(ctx))
    tox.callbackFileReceiveChunk(new AntoxOnFileReceiveChunkCallback(ctx))
    tox.callbackFileRequestChunk(new AntoxOnFileRequestChunkCallback(ctx))
    tox.callbackFileControl(new AntoxOnFileControlCallback(ctx))
    /* tox.callbackGroupTopicChange(new AntoxOnGroupTopicChangeCallback(ctx))
    tox.callbackPeerJoin(new AntoxOnPeerJoinCallback(ctx))
    tox.callbackPeerExit(new AntoxOnPeerExitCallback(ctx))
    tox.callbackGroupPeerlistUpdate(new AntoxOnGroupPeerlistUpdateCallback(ctx))
    tox.callbackGroupNickChange(new AntoxOnGroupNickChangeCallback(ctx))
    tox.callbackGroupInvite(new AntoxOnGroupInviteCallback(ctx))
    tox.callbackGroupSelfJoin(new AntoxOnGroupSelfJoinCallback(ctx))
    tox.callbackGroupJoinRejected(new AntoxOnGroupJoinRejectedCallback(ctx))
    tox.callbackGroupSelfTimeout(new AntoxOnGroupSelfTimeoutCallback(ctx))
    tox.callbackGroupMessage(new AntoxOnGroupMessageCallback(ctx)) */
    tox.callbackFriendLosslessPacket(new AntoxOnFriendLosslessPacketCallback(ctx))
  }
  def save(): Unit = {
    dataFile.saveFile(tox.save())
  }
}

