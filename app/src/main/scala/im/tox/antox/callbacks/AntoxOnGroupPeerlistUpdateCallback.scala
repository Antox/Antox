package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton

class AntoxOnGroupPeerlistUpdateCallback(private var ctx: Context) /* extends GroupPeerlistUpdateCallback */ {
  //override
  def groupPeerlistUpdate(groupNumber: Int): Unit = {
    println("peerlist updated " + groupNumber)
    for (peerNumber <- ToxSingleton.tox.getGroupPeerlist(groupNumber)) {
      ToxSingleton.getGroupPeer(groupNumber, peerNumber).name = ToxSingleton.tox.getGroupPeerName(groupNumber, peerNumber)
    }
  }
}