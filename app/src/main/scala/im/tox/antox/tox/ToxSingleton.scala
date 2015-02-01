package im.tox.antox.tox

import java.io.{BufferedReader, File, InputStreamReader, Reader}
import java.net.URL
import java.nio.charset.Charset
import java.util
import java.util.{ArrayList, HashMap}

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import im.tox.antox.callbacks._
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.utils._
import im.tox.antox.wrapper.ToxCore
import im.tox.tox4j.core.ToxOptions
import im.tox.tox4j.core.enums.{ToxStatus, ToxFileControl, ToxFileKind}
import im.tox.tox4j.exceptions.ToxException
import im.tox.tox4j.{ToxAvImpl, ToxCoreImpl}
import org.json.JSONObject
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

object ToxSingleton {

  private val TAG = "im.tox.antox.tox.ToxSingleton"

  def getInstance() = this

  var tox: ToxCore = _

  var toxAv: ToxAvImpl = _

  private var antoxFriendList: AntoxFriendList = _

  var mNotificationManager: NotificationManager = _

  var dataFile: ToxDataFile = _

  var qrFile: File = _

  var typingMap: util.HashMap[String, Boolean] = new util.HashMap[String, Boolean]()

  var isInited: Boolean = false

  var activeKey: String = _

  var chatActive: Boolean = _

  var dhtNodes: Array[DhtNode] = Array()

  def getAntoxFriend(key: String): Option[AntoxFriend] = {
    try {
      antoxFriendList.getByKey(key)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        None
      }
    }
  }

  def getAntoxFriend(friendNumber: Int): Option[AntoxFriend] = {
    try {
      antoxFriendList.getByFriendNumber(friendNumber)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        None
      }
    }
  }

  def getAntoxFriendList: AntoxFriendList = antoxFriendList

  def keyFromAddress(address: String): String = {
    address.substring(0, 64) //Cut to the length of the public key portion of a tox address. TODO: make a class that represents the full tox address
  }

  def exportDataFile(): Unit = {
    dataFile.exportFile()
  }

  def sendFileSendRequest(path: String, key: String, context: Context) {
    val file = new File(path)
    val splitPath = path.split("/")
    val fileName = splitPath(splitPath.length - 1)
    val splitFileName = fileName.span(_ != '.')
    val extension = splitFileName._2
    val name = splitFileName._1
    val nameTruncated = name.slice(0, 64 - 1 - extension.length)
    val fileNameTruncated = nameTruncated + extension
    Log.d(TAG, "sendFileSendRequest")
    if (fileName != null) {
      require(key != null)
      getAntoxFriend(key)
        .map(_.getFriendnumber)
        .flatMap(friendNumber => {
          try {
            Log.d(TAG, "Creating tox file sender")
            val fn = tox.fileSend(friendNumber, ToxFileKind.DATA, file.length(), fileName)
            fn match {
              case -1 => None
              case x => Some(x)
            }
          } catch {
            case e: Exception => {
              e.printStackTrace()
              None
            }
          }
          }).foreach(fileNumber => {
            val antoxDB = new AntoxDB(context)
            Log.d(TAG, "adding File Transfer")
            val id = antoxDB.addFileTransfer(key, path, fileNumber, file.length.toInt, sending = true)
            State.transfers.add(new FileTransfer(key, file, fileNumber, file.length, 0, true, FileStatus.REQUESTSENT, id))
            antoxDB.close()
          })
    }
  }

  def fileSendRequest(key: String,
    fileNumber: Int,
    fileName: String,
    fileSize: Long,
    context: Context) {
      Log.d(TAG, "fileSendRequest")
      var fileN = fileName
      val fileSplit = fileName.split("\\.")
      var filePre = ""
      val fileExt = fileSplit(fileSplit.length - 1)
      for (j <- 0 until fileSplit.length - 1) {
        filePre = filePre.concat(fileSplit(j))
        if (j < fileSplit.length - 2) {
          filePre = filePre.concat(".")
        }
      }
      val dirfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Constants.DOWNLOAD_DIRECTORY)
      if (!dirfile.mkdirs()) {
        Log.e("acceptFile", "Directory not created")
      }
      var file = new File(dirfile.getPath, fileN)
      if (file.exists()) {
        var i = 1
        do {
          fileN = filePre + "(" + java.lang.Integer.toString(i) + ")" +
          "." +
          fileExt
          file = new File(dirfile.getPath, fileN)
          i += 1
        } while (file.exists())
      }
      val antoxDB = new AntoxDB(context)
      val id = antoxDB.addFileTransfer(key, fileN, fileNumber, fileSize.toInt, sending = false)
      State.transfers.add(new FileTransfer(key, file, fileNumber, fileSize, 0, false, FileStatus.REQUESTSENT, id))
      antoxDB.close()
      updateMessages(context)
  }

  def changeActiveKey(key: String) {
    Reactive.activeKey.onNext(Some(key))
  }

  def clearActiveKey() {
    Reactive.activeKey.onNext(None)
  }

  private def fileAcceptOrReject(key: String, fileNumber: Integer, context: Context, accept: Boolean) {
    Log.d(TAG, "fileAcceptReject, accepting: " + accept)
    val id = State.db.getFileId(key, fileNumber)
    if (id != -1) {
      val mFriend = antoxFriendList.getByKey(key)
      mFriend.foreach(friend => {
        try {
          tox.fileControl(friend.getFriendnumber, fileNumber,
              if (accept) ToxFileControl.RESUME else ToxFileControl.CANCEL)
          
          if (accept) {
            State.db.fileTransferStarted(key, fileNumber)
          } else {
            State.db.clearFileNumber(key, fileNumber)
          } 
          
          val transfer = State.transfers.get(id)
          transfer match {
            case Some(t) =>
              if (accept) {t.status = FileStatus.INPROGRESS} else {t.status = FileStatus.CANCELLED}
            case None => 
          }
        } catch {
          case e: Exception => e.printStackTrace()
        }
      })
      Reactive.updatedMessages.onNext(true)
    }
  }

  def acceptFile(key: String, fileNumber: Int, context: Context) = fileAcceptOrReject(key, fileNumber, context, accept = true)

  def rejectFile(key: String, fileNumber: Int, context: Context) = fileAcceptOrReject(key, fileNumber, context, accept = false)

  def receiveFileData(key: String,
    fileNumber: Int,
    data: Array[Byte],
    context: Context) {
      Log.d(TAG, "receiveFileData")
      val mTransfer = State.transfers.get(key, fileNumber)
      val state = Environment.getExternalStorageState
      if (Environment.MEDIA_MOUNTED == state) {
        mTransfer match {
          case Some(t) =>
            val finished = t.writeData(data)
          case None =>
        }
      }
  }

  def getProgressSinceXAgo(id: Long, ms: Long): Option[(Long, Long)] = {
    val mTransfer = State.transfers.get(id)
    mTransfer match {
      case Some(t) => t.getProgressSinceXAgo(ms)
      case None => None
    }
  }

  def fileFinished(key: String, fileNumber: Integer, context: Context) {
    Log.d(TAG, "fileFinished")
    val transfer = State.transfers.get(key, fileNumber)
    transfer match {
      case Some(t) => {
        t.status = FileStatus.FINISHED
        val mFriend = antoxFriendList.getByKey(t.key)
        State.db.fileFinished(key, fileNumber)
        Reactive.updatedMessages.onNext(true)
      }
      case None => Log.d(TAG, "fileFinished: No transfer found")
    }
  }

  def cancelFile(key: String, fileNumber: Int, context: Context) {
    Log.d(TAG, "cancelFile")
    val db = new AntoxDB(context)
    State.transfers.remove(key, fileNumber)
    db.clearFileNumber(key, fileNumber)
    db.close()
    Reactive.updatedMessages.onNext(true)
  }

  def getProgress(id: Long): Long = {
    val mTransfer = State.transfers.get(id)
    mTransfer match {
      case Some(t) => t.progress
      case None => 0
    } 
  }

  def fileTransferStarted(key: String, fileNumber: Integer, ctx: Context) {
    Log.d(TAG, "fileTransferStarted")
    State.db.fileTransferStarted(key, fileNumber)
    val mTransfer = State.transfers.get(key, fileNumber)
  }

  def pauseFile(id: Long, ctx: Context) {
    Log.d(TAG, "pauseFile")
    val mTransfer = State.transfers.get(id)
    mTransfer match {
      case Some(t) => t.status = FileStatus.PAUSED
      case None =>
    }
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

  def clearUselessNotifications(key: String) {
    if (key != null && key != "") {
      val mFriend = antoxFriendList.getByKey(key)
      mFriend.foreach(friend => {
        try {
          mNotificationManager.cancel(friend.getFriendnumber)
        } catch {
          case e: Exception => e.printStackTrace()
        }
      })
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

  def populateAntoxFriendList(): Unit = {
    for (i <-  tox.getFriendList) {
      //this doesn't set the name, status message, status
      //or online status of the friend because they are now set during callbacks
      antoxFriendList.addFriendIfNotExists(i)
      antoxFriendList.getByFriendNumber(i).get.setKey(tox.getFriendKey(i))
    }
  }

  def initTox(ctx: Context) {
    State.db = new AntoxDB(ctx).open(writeable = true)
    antoxFriendList = new AntoxFriendList()
    qrFile = ctx.getFileStreamPath("userkey_qr.png")
    dataFile = new ToxDataFile(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val udpEnabled = preferences.getBoolean("enable_udp", false)
    val options = new ToxOptions()
    options.setUdpEnabled(udpEnabled)
    options.setIpv6Enabled(Options.ipv6Enabled)

    if (!dataFile.doesFileExist()) {
      try {
        tox = new ToxCore(antoxFriendList, options)
        dataFile.saveFile(tox.save())
        val editor = preferences.edit()
        editor.putString("tox_id", tox.getAddress)
        editor.commit()
      } catch {
        case e: ToxException => e.printStackTrace()
      }
    } else {
        try {
          tox = new ToxCore(antoxFriendList, options, dataFile.loadFile())
          val editor = preferences.edit()
          editor.putString("tox_id", tox.getAddress)
          editor.commit()
        } catch {
          case e: ToxException => e.printStackTrace()
        }
      }

      toxAv = new ToxAvImpl(tox.getTox)

      val db = new AntoxDB(ctx)
      db.setAllOffline()

      val friends = db.getFriendList
      db.close()

      for (friendNumber <- tox.getFriendList) {
        val friendKey = tox.getFriendKey(friendNumber)
        if (!db.doesFriendExist(friendKey)) {
          db.addFriend(tox.getFriendKey(friendNumber), "", "", "")
        }
      }

      if (friends.size > 0) {
        for (friend <- friends) {
          try {
            tox.addFriendNoRequest(friend.key)
          } catch {
            case e: Exception => e.printStackTrace()
          }
        }

        populateAntoxFriendList()

        for (friend <- friends) {
          antoxFriendList.updateFromFriend(friend)
        }
      }
      tox.callbackFriendMessage(new AntoxOnMessageCallback(ctx))
      tox.callbackFriendRequest(new AntoxOnFriendRequestCallback(ctx))
      tox.callbackFriendAction(new AntoxOnActionCallback(ctx))
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


      try {
        tox.setName(preferences.getString("nickname", ""))
        tox.setStatusMessage(preferences.getString("status_message", ""))
        var newStatus: ToxStatus = ToxStatus.NONE
        val newStatusString = preferences.getString("status", "")
        newStatus = UserStatus.getToxUserStatusFromString(newStatusString)
        tox.setStatus(newStatus)
      } catch {
        case e: ToxException =>
      }
      updateDhtNodes(ctx)
  }
}

