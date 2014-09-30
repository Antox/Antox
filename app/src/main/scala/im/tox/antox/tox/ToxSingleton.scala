package im.tox.antox.tox

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.AsyncTask
import android.os.Environment
import android.os.SystemClock
import android.preference.PreferenceManager
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.sql.Timestamp
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import im.tox.antox.callbacks.AntoxOnActionCallback
import im.tox.antox.callbacks.AntoxOnAudioDataCallback
import im.tox.antox.callbacks.AntoxOnAvCallbackCallback
import im.tox.antox.callbacks.AntoxOnConnectionStatusCallback
import im.tox.antox.callbacks.AntoxOnFileControlCallback
import im.tox.antox.callbacks.AntoxOnFileDataCallback
import im.tox.antox.callbacks.AntoxOnFileSendRequestCallback
import im.tox.antox.callbacks.AntoxOnFriendRequestCallback
import im.tox.antox.callbacks.AntoxOnMessageCallback
import im.tox.antox.callbacks.AntoxOnNameChangeCallback
import im.tox.antox.callbacks.AntoxOnReadReceiptCallback
import im.tox.antox.callbacks.AntoxOnStatusMessageCallback
import im.tox.antox.callbacks.AntoxOnTypingChangeCallback
import im.tox.antox.callbacks.AntoxOnUserStatusCallback
import im.tox.antox.callbacks.AntoxOnVideoDataCallback
import im.tox.antox.data.AntoxDB
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.AntoxFriendList
import im.tox.antox.utils.Constants
import im.tox.antox.utils.DHTNodeDetails
import im.tox.antox.utils.DhtNode
import im.tox.antox.utils.Friend
import im.tox.antox.utils.FriendInfo
import im.tox.antox.utils.FriendRequest
import im.tox.antox.utils.Message
import im.tox.antox.utils.Options
import im.tox.antox.utils.Triple
import im.tox.antox.utils.Tuple
import im.tox.antox.utils.UserStatus
import im.tox.jtoxcore.JTox
import im.tox.jtoxcore.ToxException
import im.tox.jtoxcore.ToxFileControl
import im.tox.jtoxcore.ToxOptions
import im.tox.jtoxcore.ToxUserStatus
import im.tox.jtoxcore.callbacks.CallbackHandler
import rx.Observable
import rx.functions.Func2
import rx.functions.Func3
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.Observable.combineLatest
import ToxSingleton._
//remove if not needed
import scala.collection.JavaConversions._

object ToxSingleton {

  private val TAG = "im.tox.antox.tox.ToxSingleton"

  def getInstance() = this

  object FileStatus extends Enumeration {
    type FileStatus = Value
    val REQUESTSENT, CANCELLED, INPROGRESS, FINISHED, PAUSED = Value
  }
  import FileStatus._

  var jTox: JTox[AntoxFriend] = _

  private var antoxFriendList: AntoxFriendList = _

  var callbackHandler: CallbackHandler[AntoxFriend] = _

  var mNotificationManager: NotificationManager = _

  var dataFile: ToxDataFile = _

  var qrFile: File = _

  var friendListSubject: BehaviorSubject[ArrayList[Friend]] = _

  var friendRequestSubject: BehaviorSubject[ArrayList[FriendRequest]] = _

  var lastMessagesSubject: BehaviorSubject[HashMap[String, Tuple[String,Timestamp]]] = _

  var unreadCountsSubject: BehaviorSubject[HashMap[String, Int]] = _

  var typingSubject: BehaviorSubject[Boolean] = _

  var activeKeySubject: BehaviorSubject[String] = _

  var updatedMessagesSubject: BehaviorSubject[Boolean] = _

  var updatedProgressSubject: BehaviorSubject[Boolean] = _

  var rightPaneActiveSubject: BehaviorSubject[Boolean] = _

  var doClosePaneSubject: PublishSubject[Boolean] = _

  var friendInfoListSubject: rx.Observable[ArrayList[FriendInfo]] = _

  var activeKeyAndIsFriendSubject: rx.Observable[Tuple[String,Boolean]] = _

  var friendListAndRequestsSubject: Observable[Tuple[ArrayList[FriendInfo],ArrayList[FriendRequest]]] = _

  var rightPaneActiveAndKeyAndIsFriendSubject: Observable[Triple[Boolean, String, Boolean]] = _

  var friendInfoListAndActiveSubject: Observable[Tuple[ArrayList[FriendInfo],Tuple[String,Boolean]]] = _

  var progressMap: HashMap[Integer, Integer] = new HashMap[Integer, Integer]()

  var progressHistoryMap: HashMap[Integer, ArrayList[Tuple[Integer, Long]]] = new HashMap[Integer, ArrayList[Tuple[Integer, Long]]]()

  var fileStatusMap: HashMap[Integer, FileStatus] = new HashMap[Integer, FileStatus]()

  var fileSizeMap: HashMap[Integer, Integer] = new HashMap[Integer, Integer]()

  var fileStreamMap: HashMap[Integer, FileOutputStream] = new HashMap[Integer, FileOutputStream]()

  var fileMap: HashMap[Integer, File] = new HashMap[Integer, File]()

  var fileIds: HashSet[Integer] = new HashSet[Integer]()

  var typingMap: HashMap[String, Boolean] = new HashMap[String, Boolean]()

  var isInited: Boolean = false

  var activeKey: String = _

  var chatActive: Boolean = _

  def getAntoxFriend(key: String): AntoxFriend = antoxFriendList.getById(key)

  def initSubjects(ctx: Context) {
    friendListSubject = BehaviorSubject.create(new ArrayList[Friend]())
    friendListSubject.subscribeOn(Schedulers.io())
    friendRequestSubject = BehaviorSubject.create(new ArrayList[FriendRequest]())
    friendRequestSubject.subscribeOn(Schedulers.io())
    rightPaneActiveSubject = BehaviorSubject.create(false)
    rightPaneActiveSubject.subscribeOn(Schedulers.io())
    lastMessagesSubject = BehaviorSubject.create(new HashMap[String, Tuple[String, Timestamp]]())
    lastMessagesSubject.subscribeOn(Schedulers.io())
    unreadCountsSubject = BehaviorSubject.create(new HashMap[String, Int]())
    unreadCountsSubject.subscribeOn(Schedulers.io())
    activeKeySubject = BehaviorSubject.create("")
    activeKeySubject.subscribeOn(Schedulers.io())
    doClosePaneSubject = PublishSubject.create()
    doClosePaneSubject.subscribeOn(Schedulers.io())
    updatedMessagesSubject = BehaviorSubject.create(true)
    updatedMessagesSubject.subscribeOn(Schedulers.io())
    updatedProgressSubject = BehaviorSubject.create(true)
    updatedProgressSubject.subscribeOn(Schedulers.io())
    typingSubject = BehaviorSubject.create(true)
    typingSubject.subscribeOn(Schedulers.io())
    friendInfoListSubject = combineLatest(friendListSubject, lastMessagesSubject, unreadCountsSubject, 
      new Func3[ArrayList[Friend], HashMap[String, Tuple[String,Timestamp]], HashMap[String, Int], ArrayList[FriendInfo]]() {

      override def call(fl: ArrayList[Friend], lm: HashMap[String, Tuple[String, Timestamp]], uc: HashMap[String, Int]): ArrayList[FriendInfo] = {
        var fi = new ArrayList[FriendInfo]()
        for (f <- fl) {
          var lastMessage: String = null
          var lastMessageTimestamp: Timestamp = null
          var unreadCount: Int = 0
          if (lm.containsKey(f.friendKey)) {
            lastMessage = lm.get(f.friendKey).asInstanceOf[Tuple[String, Timestamp]].x.asInstanceOf[String]
            lastMessageTimestamp = lm.get(f.friendKey).asInstanceOf[Tuple[String, Timestamp]].y.asInstanceOf[Timestamp]
          } else {
            lastMessage = ""
            lastMessageTimestamp = new Timestamp(0, 0, 0, 0, 0, 0, 0)
          }
          unreadCount = if (uc.containsKey(f.friendKey)) uc.get(f.friendKey).asInstanceOf[java.lang.Integer] else 0
          fi.add(new FriendInfo(f.isOnline, f.friendName, f.friendStatus, f.personalNote, f.friendKey, 
            lastMessage, lastMessageTimestamp, unreadCount, f.alias))
        }
        return fi
      }
    })
    friendListAndRequestsSubject = combineLatest(friendInfoListSubject, friendRequestSubject, new Func2[ArrayList[FriendInfo], ArrayList[FriendRequest], Tuple[ArrayList[FriendInfo], ArrayList[FriendRequest]]]() {

      override def call(fl: ArrayList[FriendInfo], fr: ArrayList[FriendRequest]): Tuple[ArrayList[FriendInfo], ArrayList[FriendRequest]] = {
        return new Tuple(fl, fr)
      }
    })
    activeKeyAndIsFriendSubject = combineLatest(activeKeySubject, friendListSubject, new Func2[String, ArrayList[Friend], Tuple[String, Boolean]]() {

      override def call(key: String, fl: ArrayList[Friend]): Tuple[String, Boolean] = {
        var isFriend: Boolean = false
        isFriend = isKeyFriend(key, fl)
        return new Tuple[String, Boolean](key, isFriend)
      }
    })
    friendInfoListAndActiveSubject = combineLatest(friendInfoListSubject, activeKeyAndIsFriendSubject, 
      new Func2[ArrayList[FriendInfo], Tuple[String, Boolean], Tuple[ArrayList[FriendInfo], Tuple[String, Boolean]]]() {

      override def call(o: ArrayList[FriendInfo], o2: Tuple[String, Boolean]): Tuple[ArrayList[FriendInfo], Tuple[String, Boolean]] = {
        return new Tuple[ArrayList[FriendInfo], Tuple[String, Boolean]](o.asInstanceOf[ArrayList[FriendInfo]], 
          o2.asInstanceOf[Tuple[String, Boolean]])
      }
    })
    rightPaneActiveAndKeyAndIsFriendSubject = combineLatest(rightPaneActiveSubject, activeKeyAndIsFriendSubject, 
      new Func2[Boolean, Tuple[String, Boolean], Triple[Boolean, String, Boolean]]() {

      override def call(rightPaneActive: Boolean, activeKeyAndIsFriend: Tuple[String, Boolean]): Triple[Boolean, String, Boolean] = {
        var activeKey = activeKeyAndIsFriend.x
        var isFriend = activeKeyAndIsFriend.y
        new Triple[Boolean, String, Boolean](rightPaneActive, activeKey, isFriend)
      }
    })
  }

  private def isKeyFriend(key: String, fl: ArrayList[Friend]): Boolean = {
    fl.find(_.friendKey == key).map(_ => true).getOrElse(false)
  }

  def sendFileSendRequest(path: String, key: String, context: Context) {
    val file = new File(path)
    val splitPath = path.split("/")
    val fileName = splitPath(splitPath.length - 1)
    Log.d("sendFileSendRequest", "name: " + fileName)
    if (fileName != null) {
      var fileNumber = -1
      try {
        fileNumber = jTox.newFileSender(getAntoxFriend(activeKey).getFriendnumber, file.length, fileName)
      } catch {
        case e: Exception => Log.d("toxNewFileSender error", e.toString)
      }
      if (fileNumber != -1) {
        val antoxDB = new AntoxDB(context)
        val id = antoxDB.addFileTransfer(key, path, fileNumber, file.length.toInt, true)
        fileIds.add(id.toInt)
        antoxDB.close()
      }
    }
  }

  def fileSendRequest(key: String, 
      fileNumber: Int, 
      fileName: String, 
      fileSize: Long, 
      context: Context) {
    Log.d("fileSendRequest, fileNumber: ", java.lang.Integer.toString(fileNumber))
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
    fileIds.add(id.toInt)
    antoxDB.close()
  }

  def changeActiveKey(key: String) {
    activeKeySubject.onNext(key)
    doClosePaneSubject.onNext(true)
  }

  def clearActiveKey() {
    activeKeySubject.onNext("")
    doClosePaneSubject.onNext(false)
  }

  def acceptFile(key: String, fileNumber: Int, context: Context) {
    val antoxDB = new AntoxDB(context)
    val id = antoxDB.getFileId(key, fileNumber)
    if (id != -1) {
      try {
        jTox.fileSendControl(antoxFriendList.getById(key).getFriendnumber, false, fileNumber, ToxFileControl.TOX_FILECONTROL_ACCEPT.ordinal(), 
          Array.ofDim[Byte](0))
      } catch {
        case e: Exception => e.printStackTrace()
      }
      antoxDB.fileTransferStarted(key, fileNumber)
      fileStatusMap.put(id, FileStatus.INPROGRESS)
    }
    antoxDB.close()
    updatedMessagesSubject.onNext(true)
  }

  def rejectFile(key: String, fileNumber: Int, context: Context) {
    val antoxDB = new AntoxDB(context)
    val id = antoxDB.getFileId(key, fileNumber)
    if (id != -1) {
      try {
        jTox.fileSendControl(antoxFriendList.getById(key).getFriendnumber, false, fileNumber, ToxFileControl.TOX_FILECONTROL_KILL.ordinal(), 
          Array.ofDim[Byte](0))
      } catch {
        case e: Exception => e.printStackTrace()
      }
      antoxDB.clearFileNumber(key, fileNumber)
      fileStatusMap.put(id, FileStatus.CANCELLED)
    }
    antoxDB.close()
    updatedMessagesSubject.onNext(true)
  }

  def receiveFileData(key: String, 
      fileNumber: Int, 
      data: Array[Byte], 
      context: Context) {
    val antoxDB = new AntoxDB(context)
    val id = antoxDB.getFileId(key, fileNumber)
    val state = Environment.getExternalStorageState
    if (Environment.MEDIA_MOUNTED == state) {
      if (!fileStreamMap.containsKey(id)) {
        val fileName = antoxDB.getFilePath(key, fileNumber)
        val dirfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), 
          Constants.DOWNLOAD_DIRECTORY)
        if (!dirfile.mkdirs()) {
          Log.e("acceptFile", "Directory not created")
        }
        val file = new File(dirfile.getPath, fileName)
        var output: FileOutputStream = null
        try {
          output = new FileOutputStream(file, true)
        } catch {
          case e: Exception => e.printStackTrace()
        }
        fileMap.put(id, file)
        fileStreamMap.put(id, output)
      }
      antoxDB.close()
      try {
        fileStreamMap.get(id).write(data)
      } catch {
        case e: Exception => e.printStackTrace()
      } finally {
        incrementProgress(id, data.length)
      }
      Log.d("ToxSingleton", "file size so far: " + fileMap.get(id).length + " final file size: " + 
        fileSizeMap.get(id))
      if (fileMap.get(id).length == fileSizeMap.get(id)) {
        try {
          fileStreamMap.get(id).close()
          jTox.fileSendControl(antoxFriendList.getById(key).getFriendnumber, false, fileNumber, ToxFileControl.TOX_FILECONTROL_FINISHED.ordinal(), 
            Array.ofDim[Byte](0))
          fileFinished(key, fileNumber, context)
          Log.d("ToxSingleton", "receiveFileData finished receiving file")
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }
    }
  }

  def incrementProgress(id: Int, length: Int) {
    val idObject = id
    if (id != -1) {
      val time = System.currentTimeMillis()
      if (!progressMap.containsKey(idObject)) {
        progressMap.put(idObject, length)
        val a = new ArrayList[Tuple[Integer, Long]]()
        a.add(new Tuple[Integer, Long](length, time))
        progressHistoryMap.put(idObject, a)
      } else {
        val current = progressMap.get(idObject)
        progressMap.put(idObject, current + length)
        val a = progressHistoryMap.get(idObject)
        a.add(new Tuple[Integer, Long](current + length, time))
        progressHistoryMap.put(idObject, a)
      }
    }
    updatedProgressSubject.onNext(true)
  }

  def getProgressSinceXAgo(id: Int, ms: Int): Tuple[Integer, Long] = {
    if (progressHistoryMap.containsKey(id)) {
      val progressHistory = progressHistoryMap.get(id)
      if (progressHistory.size <= 1) {
        return null
      }
      val current = progressHistory.get(progressHistory.size - 1)
      var before: Tuple[Integer, Long] = null
      var timeDifference: Long = 0l
      var i = progressHistory.size - 2
      while (i >= 0) {
        before = progressHistory.get(i)
        timeDifference = current.y - before.y
        if (timeDifference > ms || i == 0) {
          return new Tuple[Integer, Long](current.x - before.x, System.currentTimeMillis() - before.y)
        }
        i
      }
    }
    null
  }

  def setProgress(id: Int, progress: Int) {
    val idObject = id
    if (id != -1) {
      val time = System.currentTimeMillis()
      progressMap.put(idObject, progress)
      var a: ArrayList[Tuple[Integer, Long]] = null
      a = if (!progressHistoryMap.containsKey(idObject)) new ArrayList[Tuple[Integer, Long]]() else progressHistoryMap.get(idObject)
      a.add(new Tuple[Integer, Long](progress, time))
      progressHistoryMap.put(idObject, a)
      updatedProgressSubject.onNext(true)
    }
  }

  def fileFinished(key: String, fileNumber: Int, context: Context) {
    Log.d("ToxSingleton", "fileFinished")
    val db = new AntoxDB(context)
    val id = db.getFileId(key, fileNumber)
    if (id != -1) {
      fileStatusMap.put(id, FileStatus.FINISHED)
      fileIds.remove(id)
    }
    db.fileFinished(key, fileNumber)
    db.close()
    updatedMessagesSubject.onNext(true)
  }

  def cancelFile(key: String, fileNumber: Int, context: Context) {
    Log.d("ToxSingleton", "cancelFile")
    val db = new AntoxDB(context)
    val id = db.getFileId(key, fileNumber)
    if (id != -1) {
      fileStatusMap.put(id, FileStatus.CANCELLED)
    }
    db.clearFileNumber(key, fileNumber)
    db.close()
    updatedMessagesSubject.onNext(true)
  }

  def getProgress(id: Int): Int = {
    if (id != -1 && progressMap.containsKey(id)) {
      progressMap.get(id)
    } else {
      0
    }
  }

  def sendFileData(key: String, 
      fileNumber: Int, 
      startPosition: Int, 
      context: Context) {
    class sendFileTask extends AsyncTask[Void, Void, Void] {

      protected override def doInBackground(params: Void*): Void = {
        val result = doSendFileData(key, fileNumber, startPosition, context)
        Log.d("doSendFileData finished, result: ", java.lang.Boolean.toString(result))
        val db = new AntoxDB(context)
        db.clearFileNumber(key, fileNumber)
        db.close()
        return null
      }

      protected override def onPostExecute(result: Void) {
      }
    }
    new sendFileTask().execute()
  }

  def doSendFileData(key: String, 
      fileNumber: Int, 
      startPosition: Int, 
      context: Context): Boolean = {
    var path = ""
    val antoxDB = new AntoxDB(context)
    path = antoxDB.getFilePath(key, fileNumber)
    val id = antoxDB.getFileId(key, fileNumber)
    antoxDB.close()
    if (id != -1) {
      fileStatusMap.put(id, FileStatus.INPROGRESS)
    }
    var result = -1
    if (path != "") {
      var chunkSize = 1
      try {
        chunkSize = jTox.fileDataSize(getAntoxFriend(key).getFriendnumber)
      } catch {
        case e: Exception => e.printStackTrace()
      }
      val file = new File(path)
      val bytes = Array.ofDim[Byte](file.length.toInt)
      var buf: BufferedInputStream = null
      try {
        buf = new BufferedInputStream(new FileInputStream(file))
      } catch {
        case e: Exception => e.printStackTrace()
      }
      var i = startPosition
      if (buf != null) {
        i = startPosition
        while (i < bytes.length) {
          val data = Array.ofDim[Byte](chunkSize)
          try {
            buf.mark(chunkSize * 2)
            val read = buf.read(data, 0, chunkSize)
          } catch {
            case e: Exception => {
              e.printStackTrace()
              //break
            }
          }
          try {
            result = jTox.fileSendData(getAntoxFriend(key).getFriendnumber, fileNumber, data)
          } catch {
            case e: Exception => {
              e.printStackTrace()
              //break
            }
          }
          if (!(fileStatusMap.containsKey(id) && fileStatusMap.get(id) == FileStatus.INPROGRESS)) {
            //break
          }
          if (result == -1) {
            Log.d("sendFileDataTask", "toxFileSendData failed")
            try {
              jTox.doTox()
            } catch {
              case e: Exception => e.printStackTrace()
            }
            SystemClock.sleep(50)
            i = i - chunkSize
            try {
              buf.reset()
            } catch {
              case e: Exception => e.printStackTrace()
            }
          }
          if (i > bytes.length) {
            i = bytes.length
          }
          setProgress(id, i)
          i = i + chunkSize
        }
        try {
          buf.close()
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }
      if (result != -1 && fileStatusMap.get(id) == FileStatus.INPROGRESS) {
        try {
          Log.d("toxFileSendControl", "FINISHED")
          jTox.fileSendControl(getAntoxFriend(key).getFriendnumber, true, fileNumber, ToxFileControl.TOX_FILECONTROL_FINISHED.ordinal(), 
            Array.ofDim[Byte](0))
          fileFinished(key, fileNumber, context)
          return true
        } catch {
          case e: Exception => Log.d("toxFileSendControl error", e.toString)
        }
      } else {
        return false
      }
    }
    false
  }

  def updateFriendsList(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val friendList = antoxDB.getFriendList
      antoxDB.close()
      friendListSubject.onNext(friendList)
    } catch {
      case e: Exception => friendListSubject.onError(e)
    }
  }

  def clearUselessNotifications(key: String) {
    if (key != null && key != "") {
      try {
        mNotificationManager.cancel(getAntoxFriend(key).getFriendnumber)
      } catch {
        case e: Exception => 
      }
    }
  }

  def sendUnsentMessages(ctx: Context) {
    val db = new AntoxDB(ctx)
    val unsentMessageList = db.getUnsentMessageList
    for (i <- 0 until unsentMessageList.size) {
      var friend: AntoxFriend = null
      val id = unsentMessageList.get(i).message_id
      var sendingSucceeded = true
      try {
        friend = getAntoxFriend(unsentMessageList.get(i).key)
      } catch {
        case e: Exception => Log.d(TAG, e.toString)
      }
      try {
        if (friend != null && friend.isOnline && jTox != null) {
          jTox.sendMessage(friend, unsentMessageList.get(i).message)
        }
      } catch {
        case e: ToxException => {
          Log.d(TAG, e.toString)
          e.printStackTrace()
          sendingSucceeded = false
        }
      }
      if (sendingSucceeded) {
        db.updateUnsentMessage(id)
      }
    }
    db.close()
    updateMessages(ctx)
  }

  def updateFriendRequests(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val friendRequest = antoxDB.getFriendRequestsList
      antoxDB.close()
      friendRequestSubject.onNext(friendRequest)
    } catch {
      case e: Exception => friendRequestSubject.onError(e)
    }
  }

  def updateMessages(ctx: Context) {
    updatedMessagesSubject.onNext(true)
    updateLastMessageMap(ctx)
    updateUnreadCountMap(ctx)
  }

  def updateLastMessageMap(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val map = antoxDB.getLastMessages
      antoxDB.close()
      lastMessagesSubject.onNext(map)
    } catch {
      case e: Exception => lastMessagesSubject.onError(e)
    }
  }

  def updateUnreadCountMap(ctx: Context) {
    try {
      val antoxDB = new AntoxDB(ctx)
      val map = antoxDB.getUnreadCounts
      antoxDB.close()
      unreadCountsSubject.onNext(map)
    } catch {
      case e: Exception => unreadCountsSubject.onError(e)
    }
  }

  def initTox(ctx: Context) {
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
      for (i <- 0 until friends.size) {
        try {
          jTox.confirmRequest(friends.get(i).friendKey)
        } catch {
          case e: Exception => 
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
    val connMgr = ctx.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connMgr.getActiveNetworkInfo
    if (networkInfo != null && networkInfo.isConnected) {
      try {
        if (DhtNode.ipv4.size == 0) new DHTNodeDetails(ctx).execute().get
        for (i <- 0 until DhtNode.ipv4.size) {
          jTox.bootstrap(DhtNode.ipv4.get(i), java.lang.Integer.parseInt(DhtNode.port.get(i)), DhtNode.key.get(i))
        }
      } catch {
        case e: Exception => 
      }
    }
    isInited = true
  }
}
