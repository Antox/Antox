package im.tox.antox.data

import android.app.Activity
import android.content.{Intent, Context}
import android.preference.PreferenceManager
import im.tox.antox.activities.LoginActivity
import im.tox.antox.av.CallManager
import im.tox.antox.tox.{ToxDoService, ToxSingleton}
import im.tox.antox.transfer.FileTransferManager

import scala.collection.JavaConversions._

object State {

  private var _chatActive: Boolean = false
  private var _activeKey: Option[String] = None

  val transfers: FileTransferManager = new FileTransferManager()

  var db: AntoxDB = _

  def chatActive = _chatActive

  def chatActive(b: Boolean) = {
    _chatActive = b
  }

  def activeKey = _activeKey

  def activeKey(k: Option[String]) = {
    require(k != null)
    _activeKey = k
  }

  def logout(activity: Activity): Unit = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
    if (!preferences.getBoolean("logging_enabled", true)) {
      val db = new AntoxDB(activity)
      ToxSingleton.getAntoxFriendList.all().foreach(f => db.deleteChat(f.key))
    }

    val editor = preferences.edit()
    editor.putBoolean("loggedin", false)
    editor.apply()
    val startTox = new Intent(activity, classOf[ToxDoService])
    activity.stopService(startTox)
    val login = new Intent(activity, classOf[LoginActivity])
    activity.startActivity(login)
    activity.finish()
  }
}
