package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.utils.AntoxLog

class AntoxOnGroupNickChangeCallback(private var ctx: Context) /* extends GroupNickChangeCallback */ {
  //override
  def groupNickChange(groupNumber: Int, peerNumber: Int, nick: Array[Byte]): Unit = {
    AntoxLog.debug(s"Peer $peerNumber nick changed")
    //ToxSingleton.getGroupPeer(groupNumber, peerNumber).name = new String(nick, "UTF-8")
  }
}