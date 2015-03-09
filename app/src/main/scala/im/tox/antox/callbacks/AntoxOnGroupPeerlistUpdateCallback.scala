package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.{GroupPeerlistUpdateCallback, GroupTopicChangeCallback, GroupPeerJoinCallback}

class AntoxOnGroupPeerlistUpdateCallback(private var ctx: Context) extends GroupPeerlistUpdateCallback {
  override def groupPeerlistUpdate(groupNumber: Int): Unit = {
    ToxSingleton.getGroupList.getByGroupNumber(groupNumber).get.name = ToxSingleton.tox.getGroupName(groupNumber)
    ToxSingleton.updateGroupList(ctx)
  }
}