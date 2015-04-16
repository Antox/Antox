package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.callbacks.AntoxOnReadReceiptCallback._
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.core.callbacks.ReadReceiptCallback

object AntoxOnReadReceiptCallback {

  private val TAG = "im.tox.antox.callbacks.AntoxOnReadReceiptCallback"
}

class AntoxOnReadReceiptCallback(private var ctx: Context) extends ReadReceiptCallback {
  override def readReceipt(friendNumber: Int, messageId: Int): Unit = {
    val db = new AntoxDB(this.ctx)
    val key = db.setMessageReceived(messageId)
    Log.d(TAG, "read receipt, for key: " + key)
    db.close()
    ToxSingleton.updateMessages(ctx)
  }
}
