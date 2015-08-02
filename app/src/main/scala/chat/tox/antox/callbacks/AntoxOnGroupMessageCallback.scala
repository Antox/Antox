package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.wrapper.MessageType

class AntoxOnGroupMessageCallback(private var ctx: Context) /* extends GroupMessageCallback */ {

  //override
  def groupMessage(groupNumber: Int, peerNumber: Int, timeDelta: Int, message: Array[Byte]): Unit = {
    println("new group message callback for id " + ToxSingleton.getGroupList.getGroup(groupNumber).key)
    MessageHelper.handleGroupMessage(ctx, groupNumber, peerNumber, ToxSingleton.getGroupList.getGroup(groupNumber).key,
                                      new String(message, "UTF-8"), MessageType.GROUP_PEER)
  }
}
