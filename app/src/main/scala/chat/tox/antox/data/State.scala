package chat.tox.antox.data

import android.app.Activity
import android.content.Intent
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
  var userDb: UserDB = _

  def chatActive = _chatActive

  def chatActive(b: Boolean) = {
    _chatActive = b
  }

  def activeKey = _activeKey

  def activeKey(k: Option[ToxKey]) = {
    require(k != null)
    _activeKey = k
  }

  def logout(activity: Activity): Unit = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
    if (!preferences.getBoolean("logging_enabled", true)) {
      ToxSingleton.getAntoxFriendList.all().foreach(f => db.deleteChatLogs(f.key))
    }

    //workaround for contacts appearing offline when the DB is upgraded
    db.synchroniseWithTox(ToxSingleton.tox)

    State.db.close()
    val editor = preferences.edit()
    editor.putBoolean("loggedin", false)
    editor.apply()
    val startTox = new Intent(activity, classOf[ToxService])
    activity.stopService(startTox)
    val login = new Intent(activity, classOf[LoginActivity])
    activity.startActivity(login)
    activity.finish()
  }
}
