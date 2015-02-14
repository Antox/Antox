package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{GroupPeer, AntoxFriend}
import im.tox.tox4j.core.callbacks.FriendNameCallback

import scala.None

class AntoxOnGroupPeerJoinCallback(private var ctx: Context) {
  def groupPeerJoin(groupNumber: Int, peerNumber: Int): Unit = {
    ToxSingleton.getGroupList.getByGroupNumber(groupNumber)
      .get.peers.addGroupPeer(new GroupPeer(peerNumber))

    ToxSingleton.updateGroupList(ctx)
  }
}