package chat.tox.antox.callbacks

import android.content.Context

class AntoxOnGroupPeerlistUpdateCallback(private var ctx: Context) /* extends GroupPeerlistUpdateCallback */ {
  //override
  def groupPeerlistUpdate(groupNumber: Int): Unit = {
    /*for (peerNumber <- ToxSingleton.tox.getGroupPeerlist(groupNumber)) {
      ToxSingleton.getGroupPeer(groupNumber, peerNumber).name = ToxSingleton.tox.getGroupPeerName(groupNumber, peerNumber)
    }*/
  }
}