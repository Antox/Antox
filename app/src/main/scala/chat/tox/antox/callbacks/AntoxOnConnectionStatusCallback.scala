package chat.tox.antox.callbacks

import android.content.{Context, SharedPreferences}
import android.preference.PreferenceManager
import chat.tox.antox.data.State
import chat.tox.antox.tox.{MessageHelper, Reactive, ToxSingleton}
import chat.tox.antox.utils.{ConnectionManager, ConnectionTypeChangeListener}
import im.tox.tox4j.core.callbacks.FriendConnectionStatusCallback
import im.tox.tox4j.core.enums.ToxConnection

import scala.collection.JavaConversions._

object AntoxOnConnectionStatusCallback {

  private val TAG = "chat.tox.antox.TAG"
}

class AntoxOnConnectionStatusCallback(private var ctx: Context) extends FriendConnectionStatusCallback[Unit] {

  private val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
  private var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener = _

  def setAllStatusNone(): Unit = {
    if (!ToxSingleton.isToxConnected(preferences, ctx)) {
      for (friend <- ToxSingleton.getAntoxFriendList.all()) {
        friendConnectionStatus(friend.getFriendNumber, ToxConnection.NONE)(Unit)
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


  override def friendConnectionStatus(friendNumber: Int, connectionStatus: ToxConnection)(state: Unit): Unit = {
    val online = connectionStatus != ToxConnection.NONE

    val db = State.db
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

  }
}
