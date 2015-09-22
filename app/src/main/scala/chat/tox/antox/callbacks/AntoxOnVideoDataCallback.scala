package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log

class AntoxOnVideoDataCallback(private var ctx: Context) {

  private val TAG = "OnVideoDataCallback"

  def execute(callID: Int,
    data: Array[Byte],
    width: Int,
    height: Int) {
    Log.d(TAG, "Received a callback from: " + callID)
  }
}
