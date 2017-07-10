package chat.tox.antox.tox

import java.io._
import java.util
import java.util.Collections

import android.content.{Context, SharedPreferences}
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import chat.tox.antox.data.{AntoxDB, State}
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.{ToxCore, _}
import im.tox.core.network.Port
import im.tox.tox4j.core.data.ToxPublicKey
import im.tox.tox4j.core.enums.ToxUserStatus
import im.tox.tox4j.core.options.{ProxyOptions, ToxOptions}
import org.json.{JSONException, JSONObject}
import org.scaloid.common.LoggerTag
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{IOScheduler, NewThreadScheduler}

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
      AntoxLog.debug("Fetched new Nodefile", TAG)
      var dhtNodes: Array[DhtNode] = Array()
      val serverArray = json.getJSONArray("nodes")
      for (i <- 0 until serverArray.length) {
        val jsonObject = serverArray.getJSONObject(i)
        if (jsonObject.getBoolean("status_tcp")) {
          dhtNodes +:= new DhtNode(
            jsonObject.getString("maintainer"),
            jsonObject.getString("ipv4"),
            ToxPublicKey.unsafeFromValue(Hex.hexStringToBytes(jsonObject.getString("public_key"))),
            Port.unsafeFromInt(jsonObject.getInt("port")))
        }
      }
      dhtNodes
    })
  }


  def bootstrap(ctx: Context, ipv4: String = "", port: Int = 0, publicKey: String = "",
                updateNodes: Boolean = false): Observable[Boolean] = {
    Observable[Boolean](subscriber => {
      val TAG = LoggerTag("Bootstrap")
      if (ipv4 != "" && port != 0 && publicKey != "") {
        AntoxLog.debug(s"Using custom bootstrap node: $ipv4:$port")
        if (publicKey.length != 64 || !publicKey.matches("^[0-9A-F]+$")) {
          AntoxLog.error("Malformed tox public key", TAG)
        }
        else {
          val address = ToxPublicKey.unsafeFromValue(Hex.hexStringToBytes(publicKey))
          dhtNodes = Array(new DhtNode("Your custom bootstrap node", ipv4, address, Port.unsafeFromInt(port)))
        }
      }
      if (updateNodes) {
        dhtNodes = updateDhtNodes(ctx).toBlocking.first
      }
      if (dhtNodes.isEmpty) {
        val savedNodeFile = new File(ctx.getFilesDir, nodeFileName)
        if (!savedNodeFile.exists()) {
          AntoxLog.debug("Nodefile does not exist, fetching new one", TAG)
          dhtNodes = updateDhtNodes(ctx).toBlocking.first
        }
        else {
          AntoxLog.debug("Reading Nodefile", TAG)
          try {
            val json = JsonReader.readJsonFromFile(savedNodeFile)
            var dhtNodes: Array[DhtNode] = Array()
            val serverArray = json.getJSONArray("nodes")
            for (i <- 0 until serverArray.length) {
              val jsonObject = serverArray.getJSONObject(i)
              if (jsonObject.getBoolean("status_tcp")) {
                dhtNodes +:= new DhtNode(
                  jsonObject.getString("maintainer"),
                  jsonObject.getString("ipv4"),
                  ToxPublicKey.unsafeFromValue(Hex.hexStringToBytes(jsonObject.getString("public_key"))),
                  Port.unsafeFromInt(jsonObject.getInt("port")))
              }

            }
            this.dhtNodes = dhtNodes
          } catch {
            case e: JSONException =>
              AntoxLog.debug("Error parsing JSON for Nodefile, fetching new one", TAG)
              dhtNodes = updateDhtNodes(ctx).toBlocking.first
          }

        }
      }
      Collections.shuffle(util.Arrays.asList(dhtNodes: _*))


      AntoxLog.debug("Trying to bootstrap", TAG)
      var nodes = ""
      for (node <- dhtNodes) {
        nodes += s"Owner: ${node.owner}, Address: ${node.ipv4}:${node.port.value}, Pubkey: ${node.key.toHexString} | "
      }

      nodes = nodes.substring(0, nodes.length - 2)

      AntoxLog.debug("Current nodes: " + nodes, TAG)

      var bootstrapped = false
      State.isBootstrapped = bootstrapped

      for (i <- dhtNodes.indices) {
        try {
          if (true) {
            AntoxLog.debug(s"Bootstrapping to ${dhtNodes(i).ipv4}:${dhtNodes(i).port.value}", TAG)
            tox.bootstrap(dhtNodes(i).ipv4, dhtNodes(i).port, dhtNodes(i).key)
            bootstrapped = true
            State.isBootstrapped = bootstrapped
          }
        } catch {
          case e: Exception =>
            AntoxLog.error(s"Couldn't bootstrap to node ${dhtNodes(i).ipv4}:${dhtNodes(i).port.value}")
        }
      }
      if (bootstrapped) {
        AntoxLog.debug("Successfully bootstrapped", TAG)
        subscriber.onNext(true)
        subscriber.onCompleted()
      }
      else if (!updateNodes && ipv4 == "" && port == 0) {
        AntoxLog.debug("Could not find a node to bootstrap to, fetching new Nodefile", TAG)
        subscriber.onNext(bootstrap(ctx, updateNodes = true).toBlocking.first)
        subscriber.onCompleted()
      }
      else {
        AntoxLog.debug("Failed to bootstrap", TAG)
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

    if (proxyEnabled) {
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
    } else {
      ProxyOptions.None
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
    val options = ToxOptions(
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


      State.db = new AntoxDB(ctx, userDb.getActiveUser, tox.getSelfKey)
      val db = State.db

      toxAv = new ToxAv(tox.getTox)

      db.clearFileNumbers()
      db.setAllOffline()

      db.synchroniseWithTox(tox)

      val details = userDb.getActiveUserDetails
      tox.setName(details.nickname)
      tox.setStatusMessage(details.statusMessage)
      var newStatus: ToxUserStatus = ToxUserStatus.NONE
      val newStatusString = details.status
      newStatus = UserStatus.getToxUserStatusFromString(newStatusString)
      tox.setStatus(newStatus)

      if (preferences.getBoolean("enable_custom_node", false)) {
        val ip = preferences.getString("custom_node_address", "127.0.0.1")
        val port = preferences.getString("custom_node_port", "33445").toInt
        var address = preferences.getString("custom_node_key", "")
        if (address == "") address = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        bootstrap(ctx, ip, port, address).subscribeOn(IOScheduler())
          .observeOn(NewThreadScheduler())
          .subscribe()
      }
      else {
        bootstrap(ctx).subscribeOn(IOScheduler())
          .observeOn(NewThreadScheduler())
          .subscribe()
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }


  }

  def save(): Unit = {
    dataFile.saveFile(tox.getSaveData)
  }
}
