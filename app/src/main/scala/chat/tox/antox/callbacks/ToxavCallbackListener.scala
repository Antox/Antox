package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.wrapper.CallNumber
import im.tox.tox4j.av.callbacks.ToxAvEventListener
import im.tox.tox4j.av.enums.ToxavFriendCallState

class ToxavCallbackListener(ctx: Context) extends ToxAvEventListener[Unit] {

  val callCallback = new AntoxOnCallCallback(ctx)
  val callStateCallback = new AntoxOnCallStateCallback(ctx)
  val audioReceiveFrameCallback = new AntoxOnAudioReceiveFrameCallback(ctx)

  override def call(friendNumber: Int, audioEnabled: Boolean, videoEnabled: Boolean)(state: Unit): Unit = {
    callCallback.call(CallNumber(friendNumber), audioEnabled, videoEnabled)(Unit)
  }

  override def callState(friendNumber: Int, callState: java.util.Collection[ToxavFriendCallState])(state: Unit): Unit = {
    callStateCallback.callState(CallNumber(friendNumber), callState)(Unit)
  }

  override def audioReceiveFrame(friendNumber: Int, pcm: Array[Short], channels: Int, samplingRate : Int)(state: Unit): Unit = {
    audioReceiveFrameCallback.audioReceiveFrame(CallNumber(friendNumber), pcm, channels, samplingRate)(Unit)
  }
}
