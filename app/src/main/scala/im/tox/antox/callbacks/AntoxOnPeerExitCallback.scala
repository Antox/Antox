package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.GroupPeer
import im.tox.tox4j.core.callbacks.{GroupPeerExitCallback, GroupPeerJoinCallback}

class AntoxOnPeerExitCallback(private var ctx: Context) extends GroupPeerExitCallback {
  def groupPeerExit(groupNumber: Int, peerNumber: Int, partMessage: Array[Byte]): Unit = {
    ToxSingleton.getGroup(groupNumber).peers.removeGroupPeer(peerNumber)
    println("peer exit")
    ToxSingleton.updateGroupList(ctx)
  }
}