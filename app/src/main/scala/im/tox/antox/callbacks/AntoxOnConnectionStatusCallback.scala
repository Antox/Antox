package im.tox.antox.callbacks

import android.content.{SharedPreferences, Context}
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import im.tox.antox.data.{State, AntoxDB}
import im.tox.antox.tox.{MessageHelper, Reactive, ToxSingleton}
import im.tox.antox.utils.{ConnectionTypeChangeListener, ConnectionManager}
import im.tox.tox4j.core.callbacks.FriendConnectionStatusCallback
import im.tox.tox4j.core.enums.ToxConnection

import scala.collection.JavaConversions._

object AntoxOnConnectionStatusCallback {

  private val TAG = "im.tox.antox.TAG"
}

class AntoxOnConnectionStatusCallback(private var ctx: Context) extends FriendConnectionStatusCallback {

  private val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
  private var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener = _

  def setAllStatusNone(): Unit = {
    if (!ToxSingleton.isToxConnected(preferences, ctx)) {
      for (friend <- ToxSingleton.getAntoxFriendList.all()) {
        friendConnectionStatus(friend.getFriendNumber, ToxConnection.NONE)
      }
    }
  }

  ConnectionManager.addConnectionTypeChangeListener(new ConnectionTypeChangeListener {
    override def connectionTypeChange(connectionType: Int): Unit = {
      setAllStatusNone()
    }
  })

  preferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
    override def onSharedPreferenceChanged(prefs: SharedPreferences, key: String): Unit = {
      key match {
        case "wifi_only" =>
          setAllStatusNone()
        case _ =>
      }
    }
  }

  preferences.registerOnSharedPreferenceChangeListener(preferencesListener)


  override def friendConnectionStatus(friendNumber: Int, connectionStatus: ToxConnection): Unit = {
    val online = connectionStatus != ToxConnection.NONE

    val db = new AntoxDB(ctx)
    val friendKey = ToxSingleton.getAntoxFriend(friendNumber).get.getKey
    db.updateContactOnline(friendKey, online)
    ToxSingleton.getAntoxFriend(friendNumber).get.setOnline(online)

    if (online) {
      MessageHelper.sendUnsentMessages(ctx)
      State.transfers.updateSelfAvatar(ctx)
    } else {
      ToxSingleton.typingMap.put(friendKey, false)
      Reactive.typing.onNext(true)
    }
    ToxSingleton.updateFriendsList(ctx)
    ToxSingleton.updateMessages(ctx)

    db.close()
  }
}
