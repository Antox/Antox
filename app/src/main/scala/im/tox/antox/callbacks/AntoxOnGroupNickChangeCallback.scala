package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.ToxSingleton

class AntoxOnGroupNickChangeCallback(private var ctx: Context) /* extends GroupNickChangeCallback */ {
  //override
  def groupNickChange(groupNumber: Int, peerNumber: Int, nick: Array[Byte]): Unit = {
    println("GOT NICK CHANGE " + peerNumber)
    ToxSingleton.getGroupPeer(groupNumber, peerNumber).name = new String(nick, "UTF-8")
  }
}