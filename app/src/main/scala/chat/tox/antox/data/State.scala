package chat.tox.antox.data

import android.app.Activity
import android.content.{Context, Intent}
import android.preference.PreferenceManager
import chat.tox.antox.activities.LoginActivity
import chat.tox.antox.av.CallManager
import chat.tox.antox.tox.{ToxService, ToxSingleton}
import chat.tox.antox.transfer.FileTransferManager
import chat.tox.antox.wrapper.ToxKey

import scala.collection.JavaConversions._

object State {

  private var _chatActive: Boolean = false
  private var _activeKey: Option[ToxKey] = None

  val transfers: FileTransferManager = new FileTransferManager()
  val calls: CallManager = new CallManager()

  var db: AntoxDB = _
  private var _userDb: Option[UserDB] = None

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

  def chatActive: Boolean = _chatActive

  def chatActive(b: Boolean): Unit = {
    _chatActive = b
  }

  def activeKey: Option[ToxKey] = _activeKey

  def activeKey(k: Option[ToxKey]): Unit = {
    require(k != null)
    _activeKey = k
  }

  def login(name: String, context: Context): Unit = {
    userDb(context).login(name)
  }

  def logout(activity: Activity): Unit = {
    if (!userDb(activity).getActiveUserDetails.loggingEnabled) {
      ToxSingleton.getAntoxFriendList.all().foreach(f => db.deleteChatLogs(f.key))
    }

    val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
    preferences.edit().putString("active_user", "").commit()
    //workaround for contacts appearing offline when the DB is upgraded
    db.synchroniseWithTox(ToxSingleton.tox)

    State.db.close()
    val startTox = new Intent(activity, classOf[ToxService])
    activity.stopService(startTox)
    userDb(activity).logout()
    val login = new Intent(activity, classOf[LoginActivity])
    activity.startActivity(login)
    activity.finish()
  }
}
