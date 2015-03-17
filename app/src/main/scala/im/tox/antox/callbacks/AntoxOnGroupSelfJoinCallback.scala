package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.R
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.{PeerList, GroupPeer}
import im.tox.tox4j.core.callbacks.{GroupSelfJoinCallback, GroupPeerlistUpdateCallback, GroupTopicChangeCallback, GroupPeerJoinCallback}

class AntoxOnGroupSelfJoinCallback(private var ctx: Context) extends GroupSelfJoinCallback {
  override def groupSelfJoin(groupNumber: Int): Unit = {
    println("got self join callback")
    new AntoxOnGroupPeerlistUpdateCallback(ctx).groupPeerlistUpdate(groupNumber)
  }
}