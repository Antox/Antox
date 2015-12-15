package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.av.YuvVideoFrame
import chat.tox.antox.data.State
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.CallNumber

class AntoxOnVideoReceiveFrameCallback(private var ctx: Context) {
  def videoReceiveFrame(callNumber: CallNumber, frame: YuvVideoFrame)(state: Unit): Unit = {
    AntoxLog.debug(s"video frame received for $callNumber dimensions(${frame.width}, ${frame.height}) yuv: ${frame.y.length} ${frame.u.length} ${frame.v.length}")

    State.callManager.get(callNumber).foreach(_.onVideoFrame(frame))
  }
}
