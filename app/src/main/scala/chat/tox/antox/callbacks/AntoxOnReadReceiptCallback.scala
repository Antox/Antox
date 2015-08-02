package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.callbacks.AntoxOnReadReceiptCallback._
import chat.tox.antox.data.State
import im.tox.tox4j.core.callbacks.FriendReadReceiptCallback

object AntoxOnReadReceiptCallback {

  private val TAG = "AntoxOnReadReceiptCallback"
}

class AntoxOnReadReceiptCallback(private var ctx: Context) extends FriendReadReceiptCallback[Unit] {
  override def friendReadReceipt(friendNumber: Int, messageId: Int)(state: Unit): Unit = {
    val db = State.db
    db.setMessageReceived(messageId)
    Log.d(TAG, "read receipt, for message " + messageId)
  }
}
