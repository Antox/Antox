package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.State
import im.tox.antox.tox.ToxSingleton
import im.tox.tox4j.av.callbacks.ReceiveAudioFrameCallback

class AntoxOnReceiveAudioFrameCallback(private var ctx: Context) extends ReceiveAudioFrameCallback {
  override def receiveAudioFrame(friendNumber: Int, pcm: Array[Short], channels: Int, samplingRate: Int): Unit = {
    ToxSingleton.getAntoxFriend(friendNumber).get.call.onAudioFrame(pcm)
  }
}
