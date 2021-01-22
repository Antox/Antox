package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.av.{StridedYuvFrame, YuvFrame}
import chat.tox.antox.wrapper.CallNumber
import im.tox.tox4j.av.callbacks.ToxAvEventListener
import im.tox.tox4j.av.data.{AudioChannels, Height, SamplingRate, Width}
import im.tox.tox4j.av.enums.ToxavFriendCallState
import im.tox.tox4j.core.data.ToxFriendNumber

class ToxavCallbackListener(ctx: Context) extends ToxAvEventListener[Unit] {

  val callCallback = new AntoxOnCallCallback(ctx)
  val callStateCallback = new AntoxOnCallStateCallback(ctx)
  val audioReceiveFrameCallback = new AntoxOnAudioReceiveFrameCallback(ctx)
  val videoReceiveFrameCallback = new AntoxOnVideoReceiveFrameCallback(ctx)

  override def call(friendNumber: ToxFriendNumber, audioEnabled: Boolean, videoEnabled: Boolean)(state: Unit): Unit = {
    callCallback.call(CallNumber.fromFriendNumber(friendNumber), audioEnabled, videoEnabled)(Unit)
  }

  override def callState(friendNumber: ToxFriendNumber, callState: java.util.EnumSet[ToxavFriendCallState])(state: Unit): Unit = {
    callStateCallback.callState(CallNumber.fromFriendNumber(friendNumber), callState)(Unit)
  }

  override def audioReceiveFrame(friendNumber: ToxFriendNumber, pcm: Array[Short], channels: AudioChannels, samplingRate: SamplingRate)(state: Unit): Unit = {
    audioReceiveFrameCallback.audioReceiveFrame(CallNumber.fromFriendNumber(friendNumber), pcm, channels, samplingRate)(Unit)
  }

  override def videoFrameCachedYUV(height: Height, yStride: Int, uStride: Int, vStride: Int): Option[(Array[Byte], Array[Byte], Array[Byte])] = {
    videoReceiveFrameCallback.videoFrameCachedYUV(height, yStride, uStride, vStride)
  }

  override def videoReceiveFrame(friendNumber: ToxFriendNumber, width: Width, height: Height, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int)(state: Unit): Unit = {
    try {
      videoReceiveFrameCallback.videoReceiveFrame(CallNumber.fromFriendNumber(friendNumber), StridedYuvFrame(YuvFrame(width.value, height.value, y, u, v), yStride, uStride, vStride))(state)
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}
