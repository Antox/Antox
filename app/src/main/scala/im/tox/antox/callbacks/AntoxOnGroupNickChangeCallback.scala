package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.AntoxFriend
import im.tox.antox.wrapper.GroupPeer
import im.tox.tox4j.core.callbacks.{GroupNickChangeCallback, GroupPeerJoinCallback, FriendNameCallback}

import scala.None

class AntoxOnGroupNickChangeCallback(private var ctx: Context) extends GroupNickChangeCallback {
  override def groupNickChange(groupNumber: Int, peerNumber: Int, p3: Array[Byte]): Unit = {

  }
}