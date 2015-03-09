package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.AntoxFriend
import im.tox.antox.wrapper.GroupPeer
import im.tox.tox4j.core.callbacks.{GroupPeerJoinCallback, FriendNameCallback}

import scala.None

class AntoxOnPeerJoinCallback(private var ctx: Context) extends GroupPeerJoinCallback {
  def groupPeerJoin(groupNumber: Int, peerNumber: Int): Unit = {
    ToxSingleton.getGroupList.getByGroupNumber(groupNumber)
      .get.peers.addGroupPeer(new GroupPeer(ToxSingleton.tox.getGroupPeerName(groupNumber, peerNumber), false))
    println("new peer")
    ToxSingleton.updateGroupList(ctx)
  }
}