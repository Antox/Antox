package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton

class AntoxOnGroupSelfTimeoutCallback(private var ctx: Context) /* extends GroupSelfTimeoutCallback */ {
  //override
  def groupSelfTimeout(groupNumber: Int): Unit = {
    ToxSingleton.getGroup(groupNumber).connected = false

    val db = new AntoxDB(ctx)
    db.updateContactOnline(ToxSingleton.getGroup(groupNumber).id, false)
    db.close()
    ToxSingleton.updateGroupList(ctx)
  }
}