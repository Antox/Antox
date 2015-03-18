package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.{PeerList, GroupPeer}
import im.tox.tox4j.core.callbacks.{GroupPeerlistUpdateCallback, GroupTopicChangeCallback, GroupPeerJoinCallback}

class AntoxOnGroupPeerlistUpdateCallback(private var ctx: Context) extends GroupPeerlistUpdateCallback {
  override def groupPeerlistUpdate(groupNumber: Int): Unit = {
    val group = ToxSingleton.getGroup(groupNumber)
    if (ToxSingleton.tox.getGroupNumberPeers(groupNumber) != group.getPeerCount) {
      group.clearPeerList()
      for (peerNumber <- ToxSingleton.tox.getGroupPeerlist(groupNumber)) {
        group.addPeer(ToxSingleton.tox, peerNumber)
        val peer = ToxSingleton.getGroupPeer(groupNumber, peerNumber)
        if (peer.name == "") peer.name = ctx.getResources.getString(R.string.group_default_peer_name)
      }
    }
    group.name = ToxSingleton.tox.getGroupName(groupNumber)
    println("set name to " + group.name)
    group.topic = ToxSingleton.tox.getGroupTopic(groupNumber)
    group.connected = true


    val db = new AntoxDB(ctx)
    db.updateGroupName(group.id, group.name)
    db.updateGroupConnected(group.id, connected = true)
    db.updateGroupTopic(group.id, group.topic)
    db.close()
    ToxSingleton.updateGroupList(ctx)
  }
}