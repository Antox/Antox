package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.AntoxLog

class AntoxOnPeerExitCallback(private var ctx: Context) /* extends GroupPeerExitCallback */ {
  def groupPeerExit(groupNumber: Int, peerNumber: Int, partMessage: Array[Byte]): Unit = {
    ToxSingleton.getGroup(groupNumber).peers.removeGroupPeer(peerNumber)
    AntoxLog.debug(s"peer $peerNumber exited group $groupNumber")
  }
}