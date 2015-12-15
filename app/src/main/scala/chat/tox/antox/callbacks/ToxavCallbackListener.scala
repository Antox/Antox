package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.av.YuvVideoFrame
import chat.tox.antox.wrapper.CallNumber
import im.tox.tox4j.av.{SamplingRate, AudioChannels}
import im.tox.tox4j.av.callbacks.ToxAvEventListener
import im.tox.tox4j.av.enums.ToxavFriendCallState

class ToxavCallbackListener(ctx: Context) extends ToxAvEventListener[Unit] {

  val callCallback = new AntoxOnCallCallback(ctx)
  val callStateCallback = new AntoxOnCallStateCallback(ctx)
  val audioReceiveFrameCallback = new AntoxOnAudioReceiveFrameCallback(ctx)
  val videoReceiveFrameCallback = new AntoxOnVideoReceiveFrameCallback(ctx)

  override def call(friendNumber: Int, audioEnabled: Boolean, videoEnabled: Boolean)(state: Unit): Unit = {
    callCallback.call(CallNumber(friendNumber), audioEnabled, videoEnabled)(Unit)
  }

  override def callState(friendNumber: Int, callState: java.util.Collection[ToxavFriendCallState])(state: Unit): Unit = {
    callStateCallback.callState(CallNumber(friendNumber), callState)(Unit)
  }

  override def audioReceiveFrame(friendNumber: Int, pcm: Array[Short], channels: AudioChannels, samplingRate : SamplingRate)(state: Unit): Unit = {
    audioReceiveFrameCallback.audioReceiveFrame(CallNumber(friendNumber), pcm, channels, samplingRate)(Unit)
  }

  override def videoReceiveFrame(friendNumber: Int, width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int)(state: Unit): Unit = {
    videoReceiveFrameCallback.videoReceiveFrame(CallNumber(friendNumber), YuvVideoFrame(width, height, y, u, v, yStride, uStride, vStride))(state)
  }
}
