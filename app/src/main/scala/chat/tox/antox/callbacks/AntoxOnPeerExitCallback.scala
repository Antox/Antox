package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.tox.ToxSingleton

class AntoxOnPeerExitCallback(private var ctx: Context) /* extends GroupPeerExitCallback */ {

  private val TAG = "PeerExitCallback"

  def groupPeerExit(groupNumber: Int, peerNumber: Int, partMessage: Array[Byte]): Unit = {
    ToxSingleton.getGroup(groupNumber).peers.removeGroupPeer(peerNumber)
    Log.d(TAG, "peer exit")
  }
}