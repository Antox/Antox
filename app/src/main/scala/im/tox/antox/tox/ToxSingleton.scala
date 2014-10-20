package im.tox.antox.tox

import java.io.{BufferedInputStream, BufferedReader, File, FileInputStream, FileOutputStream, InputStreamReader, Reader}
import java.net.URL
import java.nio.charset.Charset
import java.util.{ArrayList, HashMap, HashSet}

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.{Environment, SystemClock}
import android.preference.PreferenceManager
import android.util.Log
import im.tox.antox.callbacks.{AntoxOnActionCallback, AntoxOnAudioDataCallback, AntoxOnAvCallbackCallback, AntoxOnConnectionStatusCallback, AntoxOnFileControlCallback, AntoxOnFileDataCallback, AntoxOnFileSendRequestCallback, AntoxOnFriendRequestCallback, AntoxOnMessageCallback, AntoxOnNameChangeCallback, AntoxOnReadReceiptCallback, AntoxOnStatusMessageCallback, AntoxOnTypingChangeCallback, AntoxOnUserStatusCallback, AntoxOnVideoDataCallback}
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.utils.{AntoxFriend, AntoxFriendList, Constants, DhtNode, Friend, FriendRequest, Options, Tuple, UserStatus, FileTransfer, FileStatus}
import im.tox.jtoxcore.{JTox, ToxException, ToxFileControl, ToxOptions, ToxUserStatus}
import im.tox.jtoxcore.callbacks.CallbackHandler
import org.json.JSONObject
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
//remove if not needed
import scala.collection.JavaConversions._

object ToxSingleton {

  private val TAG = "im.tox.antox.tox.ToxSingleton"

  def getInstance() = this

  var jTox: JTox[AntoxFriend] = _

  private var antoxFriendList: AntoxFriendList = _

  var callbackHandler: CallbackHandler[AntoxFriend] = _

  var mNotificationManager: NotificationManager = _

  var dataFile: ToxDataFile = _

  var qrFile: File = _

  var typingMap: HashMap[String, Boolean] = new HashMap[String, Boolean]()

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

  private def isKeyFriend(key: String, fl: ArrayList[Friend]): Boolean = {
    fl.find(_.friendKey == key).map(_ => true).getOrElse(false)
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
            val fn = jTox.newFileSender(friendNumber, file.length, fileNameTruncated)
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
            val id = antoxDB.addFileTransfer(key, path, fileNumber, file.length.toInt, true)
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
        } while (file.exists());
      }
      val antoxDB = new AntoxDB(context)
      val id = antoxDB.addFileTransfer(key, fileN, fileNumber, fileSize.toInt, false)
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

  def fileAcceptReject(key: String, fileNumber: Integer, context: Context, accept: Boolean) {
    Log.d(TAG, "fileAcceptReject, accepting: " + accept)
    val id = State.db.getFileId(key, fileNumber)
    if (id != -1) {
      val mFriend = antoxFriendList.getByKey(key)
      mFriend.foreach(friend => {
        try {
          jTox.fileSendControl(friend.getFriendnumber, false, fileNumber, if (accept) { ToxFileControl.TOX_FILECONTROL_ACCEPT.ordinal() } else { ToxFileControl.TOX_FILECONTROL_KILL.ordinal() }, Array.ofDim[Byte](0))
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

  def acceptFile(key: String, fileNumber: Int, context: Context) = fileAcceptReject(key, fileNumber, context, true)

  def rejectFile(key: String, fileNumber: Int, context: Context) = fileAcceptReject(key, fileNumber, context, false)

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

  def fileFinished(key: String, fileNumber: Integer, sending: Boolean, context: Context) {
    Log.d(TAG, "fileFinished")
    val transfer = State.transfers.get(key, fileNumber)
    transfer match {
      case Some(t) => {
        t.status = FileStatus.FINISHED
        val mFriend = antoxFriendList.getByKey(t.key)
        mFriend.foreach(friend => {
          try {
            jTox.fileSendControl(friend.getFriendnumber, sending, fileNumber, ToxFileControl.TOX_FILECONTROL_FINISHED.ordinal(), Array.ofDim[Byte](0))
          } catch {
            case e: Exception => e.printStackTrace()
          }
        })
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
    mTransfer match {
      case Some(t) => sendFileData(key, fileNumber, t.progress, ctx)
      case None =>
    }
  }

  def pauseFile(id: Long, ctx: Context) {
    Log.d(TAG, "pauseFile")
    val mTransfer = State.transfers.get(id)
    mTransfer match {
      case Some(t) => t.status = FileStatus.PAUSED
      case None =>
    }
  }

  def sendFileData(key: String,
    fileNumber: Integer,
    startPosition: Long,
    context: Context) {
      Log.d(TAG, "sendFileData")
      Observable[Boolean](subscriber => {
        doSendFileData(key, fileNumber, startPosition, context)
        Log.d(TAG, "doSendFileData finished")
        State.db.clearFileNumber(key, fileNumber)
        subscriber.onCompleted()
      }).subscribeOn(IOScheduler()).subscribe()
  }

  def doSendFileData(key: String,
    fileNumber: Integer,
    startPosition: Long,
    context: Context) = {
      Log.d(TAG, "doSendFileData")
      val mTransfer = State.transfers.get(key, fileNumber)
      mTransfer match {
        case Some(t) =>
          t.status = FileStatus.INPROGRESS
          val mFriend = antoxFriendList.getByKey(key)
          mFriend.foreach(friend => {
            try {
              val chunkSize = jTox.fileDataSize(friend.getFriendnumber)
              try {
                var i: Long = 0
                var reset = false
                while (i < (t.size/chunkSize)) {
                  val data = t.readData(reset, chunkSize)
                  reset = true
                  data match {
                    case Some(d) =>
                      val result = jTox.fileSendData(friend.getFriendnumber, fileNumber, d)
                      if (result == -1) {
                        Log.d("sendFileDataTask", "toxFileSendData failed")
                        try {
                          jTox.doTox()
                          SystemClock.sleep(50)
                        } catch {
                          case e: Exception => e.printStackTrace()
                        }
                      } else {
                        i += 1
                        reset = false
                        t.addToProgress(t.progress + chunkSize)
                      }
                    case None => 
                  }
                }
                fileFinished(t.key, t.fileNumber, true, context)
              } catch {
                case e: Exception => {
                  e.printStackTrace()
                }
              }
            } catch {
              case e: Exception => {
                e.printStackTrace()
              }
            }
          })
        case None => {
          Log.d(TAG, "Can't find file transfer")
        }
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
            sb.toString
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
              Log.e(TAG, "update dht nodes error: " + e);
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
                  jTox.bootstrap(nodes(i).ipv4, nodes(i).port, nodes(i).key)
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

  def initTox(ctx: Context) {
    State.db = new AntoxDB(ctx).open(true)
    antoxFriendList = new AntoxFriendList()
    callbackHandler = new CallbackHandler(antoxFriendList)
    qrFile = ctx.getFileStreamPath("userkey_qr.png")
    dataFile = new ToxDataFile(ctx)
    val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    val udpEnabled = preferences.getBoolean("enable_udp", false)
    val options = new ToxOptions(Options.ipv6Enabled, udpEnabled, Options.proxyEnabled)
    if (!dataFile.doesFileExist()) {
      try {
        jTox = new JTox(antoxFriendList, callbackHandler, options)
        dataFile.saveFile(jTox.save())
        val editor = preferences.edit()
        editor.putString("tox_id", jTox.getAddress)
        editor.commit()
      } catch {
        case e: ToxException => e.printStackTrace()
      }
      } else {
        try {
          jTox = new JTox(dataFile.loadFile(), antoxFriendList, callbackHandler, options)
          val editor = preferences.edit()
          editor.putString("tox_id", jTox.getAddress)
          editor.commit()
        } catch {
          case e: ToxException => e.printStackTrace()
        }
      }
      val db = new AntoxDB(ctx)
      db.setAllOffline()
      val friends = db.getFriendList
      db.close()
      if (friends.size > 0) {
        for (friend <- friends) {
          try {
            jTox.confirmRequest(friend.friendKey)
          } catch {
            case e: Exception => e.printStackTrace()
          }
        }
      }
      val antoxOnMessageCallback = new AntoxOnMessageCallback(ctx)
      val antoxOnFriendRequestCallback = new AntoxOnFriendRequestCallback(ctx)
      val antoxOnActionCallback = new AntoxOnActionCallback(ctx)
      val antoxOnConnectionStatusCallback = new AntoxOnConnectionStatusCallback(ctx)
      val antoxOnNameChangeCallback = new AntoxOnNameChangeCallback(ctx)
      val antoxOnReadReceiptCallback = new AntoxOnReadReceiptCallback(ctx)
      val antoxOnStatusMessageCallback = new AntoxOnStatusMessageCallback(ctx)
      val antoxOnUserStatusCallback = new AntoxOnUserStatusCallback(ctx)
      val antoxOnTypingChangeCallback = new AntoxOnTypingChangeCallback(ctx)
      val antoxOnFileSendRequestCallback = new AntoxOnFileSendRequestCallback(ctx)
      val antoxOnFileControlCallback = new AntoxOnFileControlCallback(ctx)
      val antoxOnFileDataCallback = new AntoxOnFileDataCallback(ctx)
      val antoxOnAudioDataCallback = new AntoxOnAudioDataCallback(ctx)
      val antoxOnAvCallbackCallback = new AntoxOnAvCallbackCallback(ctx)
      val antoxOnVideoDataCallback = new AntoxOnVideoDataCallback(ctx)
      callbackHandler.registerOnMessageCallback(antoxOnMessageCallback)
      callbackHandler.registerOnFriendRequestCallback(antoxOnFriendRequestCallback)
      callbackHandler.registerOnActionCallback(antoxOnActionCallback)
      callbackHandler.registerOnConnectionStatusCallback(antoxOnConnectionStatusCallback)
      callbackHandler.registerOnNameChangeCallback(antoxOnNameChangeCallback)
      callbackHandler.registerOnReadReceiptCallback(antoxOnReadReceiptCallback)
      callbackHandler.registerOnStatusMessageCallback(antoxOnStatusMessageCallback)
      callbackHandler.registerOnUserStatusCallback(antoxOnUserStatusCallback)
      callbackHandler.registerOnTypingChangeCallback(antoxOnTypingChangeCallback)
      callbackHandler.registerOnFileSendRequestCallback(antoxOnFileSendRequestCallback)
      callbackHandler.registerOnFileControlCallback(antoxOnFileControlCallback)
      callbackHandler.registerOnFileDataCallback(antoxOnFileDataCallback)
      callbackHandler.registerOnAudioDataCallback(antoxOnAudioDataCallback)
      callbackHandler.registerOnAvCallbackCallback(antoxOnAvCallbackCallback)
      callbackHandler.registerOnVideoDataCallback(antoxOnVideoDataCallback)
      try {
        jTox.setName(preferences.getString("nickname", ""))
        jTox.setStatusMessage(preferences.getString("status_message", ""))
        var newStatus: ToxUserStatus = null
        val newStatusString = preferences.getString("status", "")
        newStatus = UserStatus.getToxUserStatusFromString(newStatusString)
        jTox.setUserStatus(newStatus)
      } catch {
        case e: ToxException =>
      }
      updateDhtNodes(ctx)
  }
}

