package im.tox.antox.callbacks

import android.content.Context
import android.preference.PreferenceManager
import im.tox.antox.R
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.wrapper.{PeerList, GroupPeer}
import im.tox.tox4j.core.callbacks._
import im.tox.antox.utils.Constants

class AntoxOnGroupJoinRejectedCallback(private var ctx: Context) /* extends GroupJoinRejectedCallback */ {
  private var reconnecting = false

  /* override def groupJoinRejected(groupNumber: Int, reason: ToxGroupJoinRejected): Unit = {
    if (reason == ToxGroupJoinRejected.NICK_TAKEN) {
      if (ToxSingleton.tox.getGroupSelfName(groupNumber).length < Constants.MAX_NAME_LENGTH) {
        //FIXME
        //ToxSingleton.tox.setGroupSelfName(groupNumber, PreferenceManager
        //  .getDefaultSharedPreferences(ctx)
        //  .getString("nickname", ""))
        if (!reconnecting) {
          new Thread(new Runnable {
            override def run(): Unit = {
              reconnecting = true
              Thread.sleep(10000)
              ToxSingleton.tox.reconnectGroup(groupNumber)
              reconnecting = false
            }
          }).start()
        }
      }
    } else {
      println("Tox Group Join Rejected: " + reason)
    }
  } */
}