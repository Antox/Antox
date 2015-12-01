package chat.tox.antox.callbacks

import android.content.{Context, SharedPreferences}
import android.preference.PreferenceManager
import chat.tox.antox.data.State
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.utils.{ConnectionManager, ConnectionTypeChangeListener}
import chat.tox.antox.wrapper.FriendInfo
import im.tox.tox4j.core.callbacks.FriendConnectionStatusCallback
import im.tox.tox4j.core.enums.ToxConnection

class AntoxOnConnectionStatusCallback(private var ctx: Context) extends FriendConnectionStatusCallback[Unit] {

  private val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
  private var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener = _

  def setAllStatusNone(): Unit = {
    if (!ToxSingleton.isToxConnected(preferences, ctx)) {
      for (friendInfo  <- State.db.friendInfoList.toBlocking.first) {
        friendConnectionStatus(friendInfo, ToxConnection.NONE)(Unit)
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

  def friendConnectionStatus(friendInfo: FriendInfo, connectionStatus: ToxConnection)(state: Unit): Unit = {
    val online = connectionStatus != ToxConnection.NONE

    val db = State.db
    db.updateContactOnline(friendInfo.key, online)

    if (online) {
      MessageHelper.sendUnsentMessages(friendInfo.key, ctx)
      State.transfers.updateSelfAvatar(ctx)
    } else {
      ToxSingleton.typingMap.put(friendInfo.key, false)
      State.typing.onNext(true)
    }

  }
}
