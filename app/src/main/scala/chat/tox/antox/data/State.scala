package chat.tox.antox.data

import android.app.Activity
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import android.support.v4.content.IntentCompat
import chat.tox.antox.activities.LoginActivity
import chat.tox.antox.av.CallManager
import chat.tox.antox.tox.{ToxDataFile, ToxService, ToxSingleton}
import chat.tox.antox.toxme.{ToxData, ToxMe}
import chat.tox.antox.transfer.FileTransferManager
import chat.tox.antox.utils.AntoxNotificationManager
import chat.tox.antox.wrapper.ContactKey
import rx.lang.scala.subjects.BehaviorSubject

object State {

  private var _chatActive: Boolean = false
  private var _activeKey: Option[ContactKey] = None

  val chatActive = BehaviorSubject[Boolean](false)
  val chatActiveSubscription = chatActive.subscribe(x => State.setChatActive(x))
  val activeKey = BehaviorSubject[Option[ContactKey]](None)
  val activeKeySubscription = activeKey.subscribe(x => State.setActiveKey(x))
  val typing = BehaviorSubject[Boolean](false)
  var autoAcceptFt: Boolean = false
  var batterySavingMode: Boolean = true
  var isBootstrapped: Boolean = false
  var lastFileTransferAction: Long = -1
  var lastIncomingMessageAction: Long = -1
  val noBatterySavingWithActionWithinLastXSeconds = 5 * 60
  // 5min
  var MainToxService: ToxService = null

  var serviceThreadMain: Thread = null


  val transfers: FileTransferManager = new FileTransferManager()

  var db: AntoxDB = _
  private var _userDb: Option[UserDB] = None
  val callManager = new CallManager()

  def setLastIncomingMessageAction(): Unit = {
    // System.out.println("ToxService:" + "setLastIncomingMessageAction")
    lastIncomingMessageAction = System.currentTimeMillis()
  }

  def lastIncomingMessageActionInTheLast(seconds: Long): Boolean = {
    // System.out.println("ToxService:" + "lastIncomingMessageAction=" + lastIncomingMessageAction + " System.currentTimeMillis=" + System.currentTimeMillis())
    ((lastIncomingMessageAction + (seconds * 1000)) > System.currentTimeMillis())
  }

  def setLastFileTransferAction(): Unit = {
    lastFileTransferAction = System.currentTimeMillis()
  }

  def lastFileTransferActionInTheLast(seconds: Long): Boolean = {
    ((lastFileTransferAction + (seconds * 1000)) > System.currentTimeMillis())
  }

  def getAutoAcceptFt(): Boolean = {
    autoAcceptFt
  }

  def setAutoAcceptFt(b: Boolean) = {
    autoAcceptFt = b
  }

  def getBatterySavingMode(): Boolean = {
    batterySavingMode
  }

  def setBatterySavingMode(b: Boolean) = {
    batterySavingMode = b
  }

  def userDb(context: Context): UserDB = {
    _userDb match {
      case Some(userDb) =>
        userDb
      case None =>
        val db = new UserDB(context)
        _userDb = Some(db)
        db
    }
  }

  def isChatActive(chatKey: ContactKey): Boolean = {
    _chatActive && _activeKey.contains(chatKey)
  }

  def setChatActive(b: Boolean): Unit = {
    _chatActive = b
  }

  private def setActiveKey(k: Option[ContactKey]): Unit = {
    require(k != null)
    _activeKey = k
  }

  def login(name: String, context: Context): Unit = {
    userDb(context).login(name)
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (preferences.getBoolean("notifications_persistent", false)) {
      AntoxNotificationManager.createPersistentNotification(context)
    }
  }

  def logout(activity: Activity): Unit = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext)
    if (preferences.getBoolean("notifications_persistent", false)) {
      AntoxNotificationManager.removePersistentNotification()
    }

    //clear notifications as they are now invalid after logging out
    AntoxNotificationManager.clearAllNotifications()

    //remove and end all calls
    callManager.removeAndEndAll()

    if (!userDb(activity).getActiveUserDetails.loggingEnabled) {
      db.friendInfoList.toBlocking.first.foreach(f => db.deleteChatLogs(f.key))
    }

    //workaround for contacts appearing offline when the DB is upgraded
    db.synchroniseWithTox(ToxSingleton.tox)
    db.close()

    val startTox = new Intent(activity, classOf[ToxService])
    activity.stopService(startTox)
    userDb(activity).logout()
    val login = new Intent(activity, classOf[LoginActivity])
    activity.startActivity(login)
    activity.finish()
  }

  def shutdown(c: Context): Unit = {
    System.out.println("Shutdown:" + "001")
    val preferences = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext)
    if (preferences.getBoolean("notifications_persistent", false)) {
      AntoxNotificationManager.removePersistentNotification()
    }
    System.out.println("Shutdown:" + "002")

    //clear notifications as they are now invalid after logging out
    AntoxNotificationManager.clearAllNotifications()
    System.out.println("Shutdown:" + "003")

    //remove and end all calls
    callManager.removeAndEndAll()
    System.out.println("Shutdown:" + "004")

    if (!userDb(c).getActiveUserDetails.loggingEnabled) {
      db.friendInfoList.toBlocking.first.foreach(f => db.deleteChatLogs(f.key))
    }
    System.out.println("Shutdown:" + "005")

    //workaround for contacts appearing offline when the DB is upgraded
    db.synchroniseWithTox(ToxSingleton.tox)
    db.close()
    System.out.println("Shutdown:" + "006")

    val startTox = new Intent(c, classOf[ToxService])
    System.out.println("Shutdown:" + "007")
    c.stopService(startTox)
    System.out.println("Shutdown:" + "008")
    userDb(c).logout()
    System.out.println("Shutdown:" + "009")
  }


  def deleteActiveAccount(activity: Activity): Unit = {
    val userInfo = userDb(activity.getApplicationContext).getActiveUserDetails
    val dataFile = new ToxDataFile(activity.getApplicationContext, userInfo.profileName)
    val toxData = new ToxData
    toxData.fileBytes = dataFile.loadFile()
    toxData.address = ToxSingleton.tox.getAddress
    val toxMeName = userInfo.toxMeName
    if (toxMeName.domain.isDefined) {
      val observable = ToxMe.deleteAccount(toxMeName, toxData)
      observable.subscribe()
    }
    userDb(activity.getApplicationContext).deleteActiveUser()

    val preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext)
    if (preferences.getBoolean("notifications_persistent", false)) {
      AntoxNotificationManager.removePersistentNotification()
    }
    AntoxNotificationManager.clearAllNotifications()

    val startTox = new Intent(activity, classOf[ToxService])
    activity.stopService(startTox)
    val loginIntent = new Intent(activity, classOf[LoginActivity])
    loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
      Intent.FLAG_ACTIVITY_CLEAR_TOP |
      IntentCompat.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(loginIntent)
    activity.finish()
  }
}
