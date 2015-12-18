package chat.tox.antox.tox

import java.io._
import java.util

import android.content.{Context, SharedPreferences}
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import chat.tox.antox.R
import chat.tox.antox.callbacks.ToxCallbackListener
import chat.tox.antox.data.{AntoxDB, State}
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.{ToxCore, _}
import im.tox.core.network.Port
import im.tox.tox4j.core.data.ToxPublicKey
import im.tox.tox4j.core.enums.ToxUserStatus
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.impl.jni.ToxAvImpl
import org.json.JSONObject
import org.scaloid.common.LoggerTag
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

import scala.io.Source

object ToxSingleton {

  var tox: ToxCore = _

  var toxAv: ToxAvImpl = _

  private var groupList: GroupList = _

  var dataFile: ToxDataFile = _

  var qrFile: File = _

  var typingMap: util.HashMap[ContactKey, Boolean] = new util.HashMap[ContactKey, Boolean]()

  var isInited: Boolean = false

  var dhtNodes: Array[DhtNode] = Array()

  def interval: Int = {
    Math.min(State.transfers.interval, tox.interval)
  }

  def getGroupList: GroupList = groupList

  def getGroup(groupNumber: Int): Group = getGroupList.getGroup(groupNumber)

  def getGroup(groupKey: GroupKey): Group = getGroupList.getGroup(groupKey)

  def getGroupPeer(groupNumber: Int, peerNumber: Int): GroupPeer = getGroupList.getPeer(groupNumber, peerNumber)

  def changeActiveKey(key: ContactKey) {
    State.activeKey.onNext(Some(key))
  }

  def exportDataFile(dest: File): Unit = {
    dataFile.exportFile(dest)
    ToxSingleton.save()
  }

  def updateDhtNodes(ctx: Context) {
    val TAG = LoggerTag("UpdateDhtNodes")

    Observable[JSONObject](subscriber => {
      AntoxLog.debug("in observable", TAG)
      try {
        AntoxLog.debug("about to readJsonFromUrl", TAG)

        val nodeFileUrl =
          "https://build.tox.chat/job/nodefile_build_linux_x86_64_release" +
            "/lastSuccessfulBuild/artifact/Nodefile.json"
        val fileName = "Nodefile.json"

        try {
          FileUtils.writePrivateFile(fileName, JsonReader.readFromUrl(nodeFileUrl), ctx)
        } catch {
          //try to continue with stored nodefile if the nodefile is down
          case e: IOException =>
            AntoxLog.error("couldn't reach Nodefile URL", TAG)
        }

        val savedNodeFile = new File(ctx.getFilesDir, fileName)
        if (!savedNodeFile.exists()) {
          FileUtils.writePrivateFile(
            fileName,
            Source.fromInputStream(ctx.getResources.openRawResource(R.raw.nodefile)).mkString,
            ctx)
        }

        val json = JsonReader.readJsonFromFile(savedNodeFile)
        subscriber.onNext(json)
        subscriber.onCompleted()
      } catch {
        case e: Exception =>
          AntoxLog.errorException("update dht nodes error", e, TAG)
          subscriber.onError(e)
      }
    }).map(json => {
      AntoxLog.debug(json.toString, TAG)
      var dhtNodes: Array[DhtNode] = Array()
      val serverArray = json.getJSONArray("servers")
      for (i <- 0 until serverArray.length) {
        val jsonObject = serverArray.getJSONObject(i)
        dhtNodes +:= new DhtNode(
          jsonObject.getString("owner"),
          jsonObject.getString("ipv6"),
          jsonObject.getString("ipv4"),
          ToxPublicKey.unsafeFromValue(Hex.hexStringToBytes(jsonObject.getString("pubkey"))),
          Port.unsafeFromInt(jsonObject.getInt("port")))
      }
      dhtNodes
    }).subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(nodes => {
      dhtNodes = nodes
      AntoxLog.debug("Trying to bootstrap", TAG)
      try {
        for (i <- nodes.indices) {
          tox.bootstrap(nodes(i).ipv4, nodes(i).port, nodes(i).key)
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
      AntoxLog.debug("Successfully bootstrapped", TAG)
    }, error => {
      AntoxLog.errorException("Failed bootstrapping", error, TAG)
    })
  }

  def isToxConnected(preferences: SharedPreferences, context: Context): Boolean = {
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val wifiOnly = preferences.getBoolean("wifi_only", true)
    val wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

    !(wifiOnly && !wifiInfo.isConnected)
  }

  def initTox(ctx: Context) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    val userDb = State.userDb(ctx)

    groupList = new GroupList()

    qrFile = ctx.getFileStreamPath("userkey_qr.png")
    dataFile = new ToxDataFile(ctx, userDb.getActiveUser)

    val udpEnabled = preferences.getBoolean("enable_udp", false)
    val options = new ToxOptions(
      udpEnabled,
      Options.ipv6Enabled,
      saveData = dataFile.loadAsSaveType())

    try {
      tox = new ToxCore(groupList, options)
      if (!dataFile.doesFileExist()) dataFile.saveFile(tox.getSaveData)
      val editor = preferences.edit()
      editor.putString("tox_id", tox.getAddress.toString)
      editor.commit()
    } catch {
      case e: ToxException[_] => e.printStackTrace()
    }

    State.db = new AntoxDB(ctx, userDb.getActiveUser, tox.getSelfKey)
    val db = State.db

    //toxAv = new ToxAvImpl(tox.getTox)

    db.clearFileNumbers()
    db.setAllOffline()

    db.synchroniseWithTox(tox)

    try {
      val details = userDb.getActiveUserDetails
      tox.setName(details.nickname)
      tox.setStatusMessage(details.statusMessage)
      var newStatus: ToxUserStatus = ToxUserStatus.NONE
      val newStatusString = details.status
      newStatus = UserStatus.getToxUserStatusFromString(newStatusString)
      tox.setStatus(newStatus)
    } catch {
      case e: ToxException[_] =>
    } finally {
    }
    updateDhtNodes(ctx)
  }

  def save(): Unit = {
    dataFile.saveFile(tox.getSaveData)
  }
}
