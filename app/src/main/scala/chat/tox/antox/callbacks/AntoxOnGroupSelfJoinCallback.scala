package chat.tox.antox.callbacks

import android.content.Context

class AntoxOnGroupSelfJoinCallback(private var ctx: Context) /* extends GroupSelfJoinCallback */ {
  //override
  def groupSelfJoin(groupNumber: Int): Unit = {
    /*new Thread(new Runnable {
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
        AntoxLog.debug("set group name to " + group.name)
        group.topic = ToxSingleton.tox.getGroupTopic(groupNumber)
        group.connected = true


        val db = State.db
        db.updateContactName(group.key, group.name)
        db.updateContactOnline(group.key, online = true)
        db.updateContactStatusMessage(group.key, group.topic)
      }
    }).start() */
  }
}