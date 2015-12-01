package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.MessageHelper
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.{GroupInfo, GroupPeer}
import im.tox.tox4j.core.enums.ToxMessageType

class AntoxOnGroupMessageCallback(private var ctx: Context) /* extends GroupMessageCallback */ {

  //override
  def groupMessage(groupInfo: GroupInfo, peerInfo: GroupPeer, timeDelta: Int, message: Array[Byte]): Unit = {
    AntoxLog.debug("new group message callback for id " + groupInfo.key)
    MessageHelper.handleGroupMessage(ctx, groupInfo, peerInfo, new String(message, "UTF-8"), ToxMessageType.NORMAL)
  }
}
