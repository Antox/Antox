package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.R
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.GroupPeer
import im.tox.tox4j.core.callbacks.{GroupPeerlistUpdateCallback, GroupTopicChangeCallback, GroupPeerJoinCallback}

class AntoxOnGroupPeerlistUpdateCallback(private var ctx: Context) extends GroupPeerlistUpdateCallback {
  override def groupPeerlistUpdate(groupNumber: Int): Unit = {
    if (ToxSingleton.tox.getGroupNumberPeers(groupNumber) != ToxSingleton.getGroup(groupNumber).getPeerCount) {
      for (peerNumber <- ToxSingleton.tox.getGroupPeerlist(groupNumber)) {
        ToxSingleton.getGroup(groupNumber).addPeer(ToxSingleton.tox, peerNumber)
        val peer = ToxSingleton.getGroupPeer(groupNumber, peerNumber)
        if (peer.name == "") peer.name = ctx.getResources.getString(R.string.group_default_peer_name)
      }
    }
    ToxSingleton.getGroup(groupNumber).name = ToxSingleton.tox.getGroupName(groupNumber)
    ToxSingleton.getGroup(groupNumber).topic = ToxSingleton.tox.getGroupTopic(groupNumber)
    ToxSingleton.updateGroupList(ctx)
  }
}