package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton

class AntoxOnPeerJoinCallback(private var ctx: Context) /* extends GroupPeerJoinCallback */ {
  def groupPeerJoin(groupNumber: Int, peerNumber: Int): Unit = {
    ToxSingleton.getGroup(groupNumber).addPeer(ToxSingleton.tox, peerNumber)
    println("new peer")
    ToxSingleton.updateGroupList(ctx)
  }
}