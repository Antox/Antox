package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antoxnightly.R

class AntoxOnGroupSelfJoinCallback(private var ctx: Context) /* extends GroupSelfJoinCallback */ {
  //override
  def groupSelfJoin(groupNumber: Int): Unit = {
    println("got self join callback")
    new Thread(new Runnable {
      override def run(): Unit = {
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
        db.updateContactName(group.id, group.name)
        db.updateContactOnline(group.id, online = true)
        db.updateContactStatusMessage(group.id, group.topic)
        db.close()
        ToxSingleton.updateGroupList(ctx)
      }
    }).start()
  }
}