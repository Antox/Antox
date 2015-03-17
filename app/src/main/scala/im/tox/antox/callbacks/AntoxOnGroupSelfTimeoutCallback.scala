package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.Constants
import im.tox.tox4j.core.callbacks._
import im.tox.tox4j.core.enums.ToxGroupJoinRejected

class AntoxOnGroupSelfTimeoutCallback(private var ctx: Context) extends GroupSelfTimeoutCallback {
  override def groupSelfTimeout(groupNumber: Int): Unit = {
    ToxSingleton.getGroup(groupNumber).connected = false

    val db = new AntoxDB(ctx)
    db.updateGroupConnected(ToxSingleton.getGroup(groupNumber).id, false)
    db.close()
    ToxSingleton.updateGroupList(ctx)
  }
}