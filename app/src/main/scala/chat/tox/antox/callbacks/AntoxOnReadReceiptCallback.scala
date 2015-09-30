package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.data.State
import chat.tox.antox.utils.AntoxLog
import im.tox.tox4j.core.callbacks.FriendReadReceiptCallback

class AntoxOnReadReceiptCallback(private var ctx: Context) extends FriendReadReceiptCallback[Unit] {
  override def friendReadReceipt(friendNumber: Int, messageId: Int)(state: Unit): Unit = {
    val db = State.db
    db.setMessageReceived(messageId)
    AntoxLog.debug("read receipt, for message " + messageId)
  }
}
