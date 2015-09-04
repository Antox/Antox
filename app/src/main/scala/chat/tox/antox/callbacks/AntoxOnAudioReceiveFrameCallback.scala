package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.tox.ToxSingleton
import im.tox.tox4j.av.callbacks.AudioReceiveFrameCallback

class AntoxOnAudioReceiveFrameCallback(private var ctx: Context) extends AudioReceiveFrameCallback {

  override def receiveAudioFrame(friendNumber: Int, pcm: Array[Short], channels: Int, sampleRate: Int)(state: Unit): Unit = {
    ToxSingleton.getAntoxFriend(friendNumber).get.call.onAudioFrame(pcm, channels, sampleRate)
  }
}
