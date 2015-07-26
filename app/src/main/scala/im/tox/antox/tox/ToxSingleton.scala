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
import im.tox.tox4j.core.enums.ToxUserStatus
import im.tox.tox4j.core.options.SaveDataOptions.ToxSave
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.impl.jni.ToxAvImpl
import org.json.JSONObject
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

object ToxSingleton {

  private val TAG = "im.tox.antox.tox.ToxSingleton"

  def getInstance() = this

  var tox: ToxCore = _

  var toxAv: ToxAvImpl = _

  private var antoxFriendList: AntoxFriendList = _

  private var groupList: GroupList = _

  var mNotificationManager: NotificationManager = _

  var dataFile: ToxDataFile = _

  var qrFile: File = _

  var typingMap: util.HashMap[ToxKey, Boolean] = new util.HashMap[ToxKey, Boolean]()

  var isInited: Boolean = false

  var activeKey: String = _

  var chatActive: Boolean = _

  var dhtNodes: Array[DhtNode] = Array()

  def interval: Int = {
    Math.min(State.transfers.interval, tox.interval)
  }

  def getAntoxFriendList: AntoxFriendList = antoxFriendList

  def getAntoxFriend(key: ToxKey): Option[Friend] = {
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

  def getGroup(groupKey: ToxKey): Group = getGroupList.getGroup(groupKey)

  def getGroupPeer(groupNumber: Int, peerNumber: Int): GroupPeer = getGroupList.getPeer(groupNumber, peerNumber)

  def changeActiveKey(key: ToxKey) {
    Reactive.activeKey.onNext(Some(key))
  }

  def clearActiveKey() {
    Reactive.activeKey.onNext(None)
  }

  def exportDataFile(dest: File): Unit = {
    dataFile.exportFile(dest)
    ToxSingleton.save()
  }

  def clearUselessNotifications(key: ToxKey) {
    val mFriend = getAntoxFriend(key)
     mFriend.foreach(friend => {
       try {
         if (mNotificationManager != null) mNotificationManager.cancel(friend.getFriendNumber)
       } catch {
         case e: Exception => e.printStackTrace()
       }
     })
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
                new ToxKey(jsonObject.getString("pubkey")),
                jsonObject.getInt("port"))
            }
            dhtNodes
          }).subscribeOn(IOScheduler())
            .observeOn(AndroidMainThreadScheduler())
            .subscribe(nodes => {
              dhtNodes = nodes
              Log.d(TAG, "Trying to bootstrap")
              try {
                for (i <- nodes.indices) {
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
    for (friendNumber <- tox.getFriendList) {
      antoxFriendList.addFriendIfNotExists(friendNumber)
      antoxFriendList.getByFriendNumber(friendNumber).get.setKey(tox.getFriendKey(friendNumber))
    }

    for (groupNumber <- tox.getGroupList) {
      val groupKey = tox.getGroupKey(groupNumber)
      val groupInfo = db.getGroupInfo(groupKey)
      groupList.addGroupIfNotExists(new Group(groupKey, groupNumber, groupInfo.name, groupInfo.alias, groupInfo.topic, new PeerList()))
    }
  }

  def initTox(ctx: Context) {
    State.db = new AntoxDB(ctx)
    antoxFriendList = new AntoxFriendList()
    groupList = new GroupList()
    qrFile = ctx.getFileStreamPath("userkey_qr.png")
    dataFile = new ToxDataFile(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val udpEnabled = preferences.getBoolean("enable_udp", false)
    val options = new ToxOptions(
      udpEnabled,
      Options.ipv6Enabled,
      saveData = dataFile.loadAsSaveType())

    try {
      tox = new ToxCore(antoxFriendList, groupList, options)
      if (!dataFile.doesFileExist()) dataFile.saveFile(tox.getSaveData)
      val editor = preferences.edit()
      editor.putString("tox_id", tox.getAddress.toString)
      editor.commit()
    } catch {
      case e: ToxException[_] => e.printStackTrace()
    }

    //toxAv = new ToxAvImpl(tox.getTox)

    val db = new AntoxDB(ctx)
    db.setAllOffline()

    db.friendList.first.subscribe(friends => {
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
    })

    db.synchroniseWithTox(tox)
    populateAntoxLists(db)

    registerCallbacks(ctx)

    try {
      tox.setName(preferences.getString("nickname", ""))
      tox.setStatusMessage(preferences.getString("status_message", ""))
      var newStatus: ToxUserStatus = ToxUserStatus.NONE
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
    tox.callbackSelfConnectionStatus(new AntoxOnSelfConnectionStatusCallback(ctx))
    tox.callbackFriendMessage(new AntoxOnMessageCallback(ctx))
    tox.callbackFriendRequest(new AntoxOnFriendRequestCallback(ctx))
    tox.callbackFriendConnectionStatus(new AntoxOnConnectionStatusCallback(ctx))
    tox.callbackFriendName(new AntoxOnNameChangeCallback(ctx))
    tox.callbackReadReceipt(new AntoxOnReadReceiptCallback(ctx))
    tox.callbackFriendStatusMessage(new AntoxOnStatusMessageCallback(ctx))
    tox.callbackFriendStatus(new AntoxOnUserStatusCallback(ctx))
    tox.callbackFriendTyping(new AntoxOnTypingChangeCallback(ctx))
    tox.callbackFileReceive(new AntoxOnFileRecvCallback(ctx))
    tox.callbackFileReceiveChunk(new AntoxOnFileRecvChunkCallback(ctx))
    tox.callbackFileRequestChunk(new AntoxOnFileChunkRequestCallback(ctx))
    tox.callbackFileControl(new AntoxOnFileRecvControlCallback(ctx))
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
    dataFile.saveFile(tox.getSaveData)
  }
}

