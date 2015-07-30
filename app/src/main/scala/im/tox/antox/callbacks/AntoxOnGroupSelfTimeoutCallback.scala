package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.{State, AntoxDB}
import im.tox.antox.tox.ToxSingleton

class AntoxOnGroupSelfTimeoutCallback(private var ctx: Context) /* extends GroupSelfTimeoutCallback */ {
  //override
  def groupSelfTimeout(groupNumber: Int): Unit = {
    ToxSingleton.getGroup(groupNumber).connected = false

    val db = State.db
    db.updateContactOnline(ToxSingleton.getGroup(groupNumber).key, false)
  }
}