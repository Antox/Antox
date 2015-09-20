package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.tox.ToxSingleton

class AntoxOnGroupNickChangeCallback(private var ctx: Context) /* extends GroupNickChangeCallback */ {
  //override
  def groupNickChange(groupNumber: Int, peerNumber: Int, nick: Array[Byte]): Unit = {
    Log.d("GroupNickChangeCallback", "GOT NICK CHANGE " + peerNumber)
    ToxSingleton.getGroupPeer(groupNumber, peerNumber).name = new String(nick, "UTF-8")
  }
}