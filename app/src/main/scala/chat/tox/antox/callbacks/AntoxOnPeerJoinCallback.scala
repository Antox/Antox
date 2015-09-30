package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.AntoxLog

class AntoxOnPeerJoinCallback(private var ctx: Context) /* extends GroupPeerJoinCallback */ {
  def groupPeerJoin(groupNumber: Int, peerNumber: Int): Unit = {
    ToxSingleton.getGroup(groupNumber).addPeer(ToxSingleton.tox, peerNumber)
    AntoxLog.debug(s"peer $peerNumber joined group $groupNumber")
  }
}