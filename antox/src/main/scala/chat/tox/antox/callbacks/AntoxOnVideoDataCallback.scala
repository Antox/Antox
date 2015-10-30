package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log

class AntoxOnVideoDataCallback(private var ctx: Context) {

  def execute(callID: Int,
    data: Array[Byte],
    width: Int,
    height: Int): Unit = {
  // do nothing
  }
}
