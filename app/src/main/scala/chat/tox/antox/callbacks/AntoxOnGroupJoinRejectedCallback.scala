package chat.tox.antox.callbacks

import android.content.Context

class AntoxOnGroupJoinRejectedCallback(private var ctx: Context) /* extends GroupJoinRejectedCallback */ {
  private var reconnecting = false

  /* override def groupJoinRejected(groupNumber: Int, reason: ToxGroupJoinRejected): Unit = {
    if (reason == ToxGroupJoinRejected.NICK_TAKEN) {
      if (ToxSingleton.tox.getGroupSelfName(groupNumber).length < Constants.MAX_NAME_LENGTH) {
        //FIXME
        //ToxSingleton.tox.setGroupSelfName(groupNumber, name)
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