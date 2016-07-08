package chat.tox.antox.tox

import java.io._
import java.util

import android.content.{Context, SharedPreferences}
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.widget.Toast
import chat.tox.antox.R
import chat.tox.antox.data.{AntoxDB, State}
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.{ToxCore, _}
import im.tox.core.network.Port
import im.tox.tox4j.core.data.ToxPublicKey
import im.tox.tox4j.core.enums.ToxUserStatus
import im.tox.tox4j.core.options.{ProxyOptions, ToxOptions}
import im.tox.tox4j.exceptions.ToxException
import org.json.{JSONException, JSONObject}
import org.scaloid.common.LoggerTag
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{IOScheduler, NewThreadScheduler}

import scala.io.Source

object ToxSingleton {

  var tox: ToxCore = _

  var toxAv: ToxAv = _

  private var groupList: GroupList = _

  var dataFile: ToxDataFile = _

  var qrFile: File = _

  var typingMap: util.HashMap[ContactKey, Boolean] = new util.HashMap[ContactKey, Boolean]()

  var isInited: Boolean = false

  var dhtNodes: Array[DhtNode] = Array()

  private val nodeFileName = "Nodefile.json"


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

  def updateDhtNodes(ctx: Context): Observable[Array[DhtNode]] = {
    val TAG = LoggerTag("UpdateDhtNodes")

    Observable[JSONObject](subscriber => {
      AntoxLog.debug("in observable", TAG)
      try {
        AntoxLog.debug("about to readJsonFromUrl", TAG)

        val nodeFileUrl =
          "https://nodes.tox.chat/json"

        try {
          FileUtils.writePrivateFile(nodeFileName, JsonReader.readFromUrl(nodeFileUrl), ctx)
        } catch {
          //try to continue with stored nodefile if the nodefile is down
          case e: IOException =>
            AntoxLog.error("couldn't reach Nodefile URL", TAG)
        }

        val savedNodeFile = new File(ctx.getFilesDir, nodeFileName)


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
      val serverArray = json.getJSONArray("nodes")
      for (i <- 0 until serverArray.length) {
        val jsonObject = serverArray.getJSONObject(i)
        dhtNodes +:= new DhtNode(
          jsonObject.getString("maintainer"),
          jsonObject.getString("ipv6"),
          jsonObject.getString("ipv4"),
          ToxPublicKey.unsafeFromValue(Hex.hexStringToBytes(jsonObject.getString("public_key"))),
          Port.unsafeFromInt(jsonObject.getInt("port")))
      }
      dhtNodes
    })
  }

  def bootstrap(ctx: Context, updateNodes: Boolean = false): Observable[Boolean] = {
    Observable[Boolean](subscriber => {
      if(updateNodes){
        dhtNodes = updateDhtNodes(ctx).toBlocking.first
      }
      val TAG = LoggerTag("Bootstrap")
      if (dhtNodes.isEmpty) {
        val savedNodeFile = new File(ctx.getFilesDir, nodeFileName)
        if (!savedNodeFile.exists()) {
          dhtNodes = updateDhtNodes(ctx).toBlocking.first
        }
        else {
          try{
            val json = JsonReader.readJsonFromFile(savedNodeFile)
            AntoxLog.debug(json.toString, TAG)
            var dhtNodes: Array[DhtNode] = Array()
            val serverArray = json.getJSONArray("nodes")
            for (i <- 0 until serverArray.length) {
              val jsonObject = serverArray.getJSONObject(i)
              dhtNodes +:= new DhtNode(
                jsonObject.getString("maintainer"),
                jsonObject.getString("ipv6"),
                jsonObject.getString("ipv4"),
                ToxPublicKey.unsafeFromValue(Hex.hexStringToBytes(jsonObject.getString("public_key"))),
                Port.unsafeFromInt(jsonObject.getInt("port")))
            }
          } catch {
            case e: JSONException =>
              dhtNodes = updateDhtNodes(ctx).toBlocking.first
          }

        }
      }
      AntoxLog.debug("Trying to bootstrap", TAG)
      var bootstrapped = false
      for (i <- dhtNodes.indices) {
        try{
          tox.bootstrap(dhtNodes(i).ipv4, dhtNodes(i).port, dhtNodes(i).key)
          bootstrapped = true
        } catch{
          case e: Exception =>
            AntoxLog.error(s"Couldn't bootstrap to node ${dhtNodes(i).ipv4}")
        }
      }
      if(bootstrapped){
        AntoxLog.debug("Successfully bootstrapped", TAG)
        subscriber.onNext(true)
        subscriber.onCompleted()
      }
      else if(!updateNodes){
        subscriber.onNext(bootstrap(ctx, updateNodes = true).toBlocking.first)
        subscriber.onCompleted()
      }
      else {
        subscriber.onNext(false)
        subscriber.onCompleted()
      }
    })
  }

  def isToxConnected(preferences: SharedPreferences, context: Context): Boolean = {
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val wifiOnly = preferences.getBoolean("wifi_only", true)
    val wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

    !(wifiOnly && !wifiInfo.isConnected)
  }

  private def readProxyOptions(preferences: SharedPreferences): ProxyOptions = {
    val TAG = LoggerTag("readProxyOptions")
    AntoxLog.verbose("Reading proxy settings", TAG)

    val proxyEnabled = preferences.getBoolean("enable_proxy", Options.proxyEnabled)
    AntoxLog.verbose("Proxy enabled: " + proxyEnabled.toString, TAG)
    if (!proxyEnabled) {
      return ProxyOptions.None
    }

    val proxyAddress = preferences.getString("proxy_address", Options.proxyAddress)
    AntoxLog.verbose("Proxy address: " + proxyAddress, TAG)

    val proxyPort = preferences.getString("proxy_port", Options.proxyPort).toInt
    AntoxLog.verbose("Proxy port: " + proxyPort, TAG)

    val proxyType = preferences.getString("proxy_type", "SOCKS5")
    AntoxLog.verbose("Proxy type: " + proxyType, TAG)
    proxyType match {
      case "HTTP" =>
        ProxyOptions.Http(proxyAddress, proxyPort)
      case "SOCKS5" =>
        ProxyOptions.Socks5(proxyAddress, proxyPort)
    }
  }

  def initTox(ctx: Context) {
    isInited = true
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    val userDb = State.userDb(ctx)

    groupList = new GroupList()

    qrFile = ctx.getFileStreamPath("userkey_qr.png")
    dataFile = new ToxDataFile(ctx, userDb.getActiveUser)

    val udpEnabled = preferences.getBoolean("enable_udp", false)
    val proxyOptions = readProxyOptions(preferences)
    val options = new ToxOptions(
      ipv6Enabled = Options.ipv6Enabled,
      proxy = proxyOptions,
      udpEnabled = udpEnabled,
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

    toxAv = new ToxAv(tox.getTox)

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
    }
    bootstrap(ctx).subscribeOn(IOScheduler())
      .observeOn(NewThreadScheduler())
      .subscribe()
  }

  def save(): Unit = {
    dataFile.saveFile(tox.getSaveData)
  }
}
