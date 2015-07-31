package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.callbacks.AntoxOnReadReceiptCallback._
import im.tox.antox.data.{State, AntoxDB}
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.FriendReadReceiptCallback

object AntoxOnReadReceiptCallback {

  private val TAG = "im.tox.antox.callbacks.AntoxOnReadReceiptCallback"
}

class AntoxOnReadReceiptCallback(private var ctx: Context) extends FriendReadReceiptCallback[Unit] {
  override def friendReadReceipt(friendNumber: Int, messageId: Int)(state: Unit): Unit = {
    val db = State.db
    db.setMessageReceived(messageId)
    Log.d(TAG, "read receipt, for message " + messageId)
  }
}
