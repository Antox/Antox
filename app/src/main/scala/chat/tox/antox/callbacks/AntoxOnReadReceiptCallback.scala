package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.FriendInfo

class AntoxOnReadReceiptCallback(private var ctx: Context) {
  def friendReadReceipt(friendInfo: FriendInfo, messageId: Int)(state: Unit): Unit = {
    val db = State.db
    db.setMessageReceived(messageId)
    AntoxLog.debug("read receipt, for message " + messageId)
  }
}
