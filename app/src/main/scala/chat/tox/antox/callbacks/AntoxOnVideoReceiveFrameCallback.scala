package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.av.{VideoFrame, FormatConversions}
import chat.tox.antox.data.State
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.CallNumber

class AntoxOnVideoReceiveFrameCallback(private var ctx: Context) {
  def videoReceiveFrame(callNumber: CallNumber, width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int)(state: Unit): Unit = {
    AntoxLog.debug(s"video frame received for $callNumber dimentions($width, $height) yuv: ${y.length} ${u.length} ${v.length}")
    val r: Array[Int] = new Array(width * height)
    val g: Array[Int] = new Array(width * height)
    val b: Array[Int] = new Array(width * height)

    for (i <- 0 until height; j <- 0 until width) {
      val yx: Int = y((i * yStride) + j) & 0xff
      val ux: Int = u(((i / 2) * uStride) + (j / 2)) & 0xff
      val vx: Int = v(((i / 2) * vStride) + (j / 2)) & 0xff

      val currPos = (i * width) + j

      r(currPos) = FormatConversions.YUVtoR(yx, ux, vx)
      g(currPos) = FormatConversions.YUVtoG(yx, ux, vx)
      b(currPos) = FormatConversions.YUVtoB(yx, ux, vx)
    }

    State.callManager.get(callNumber).foreach(_.onVideoFrame(VideoFrame(width, height, r, g, b)))
  }
}
