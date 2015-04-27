package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.tox.{MessageHelper, ToxSingleton}
import im.tox.antox.wrapper.MessageType

class AntoxOnGroupMessageCallback(private var ctx: Context) /* extends GroupMessageCallback */ {

  //override
  def groupMessage(groupNumber: Int, peerNumber: Int, timeDelta: Int, message: Array[Byte]): Unit = {
    println("new group message callback for id " + ToxSingleton.getGroupList.getGroup(groupNumber).id)
    MessageHelper.handleGroupMessage(ctx, groupNumber, peerNumber, ToxSingleton.getGroupList.getGroup(groupNumber).id,
                                      new String(message, "UTF-8"), MessageType.GROUP_PEER)
  }
}
