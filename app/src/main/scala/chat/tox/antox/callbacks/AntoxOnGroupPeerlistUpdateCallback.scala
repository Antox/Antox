package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.tox.ToxSingleton

class AntoxOnGroupPeerlistUpdateCallback(private var ctx: Context) /* extends GroupPeerlistUpdateCallback */ {
  //override
  def groupPeerlistUpdate(groupNumber: Int): Unit = {
    Log.d("AntoxOnGroupPeerlistUpdateCallback","peerlist updated " + groupNumber)
    for (peerNumber <- ToxSingleton.tox.getGroupPeerlist(groupNumber)) {
      ToxSingleton.getGroupPeer(groupNumber, peerNumber).name = ToxSingleton.tox.getGroupPeerName(groupNumber, peerNumber)
    }
  }
}