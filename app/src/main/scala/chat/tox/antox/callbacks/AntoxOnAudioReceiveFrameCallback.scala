package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.CallNumber
import im.tox.tox4j.av.{SamplingRate, AudioChannels}

class AntoxOnAudioReceiveFrameCallback(private var ctx: Context) {

  var lastReceivedFrame: Long = 0
  def audioReceiveFrame(callNumber: CallNumber, pcm: Array[Short], channels: AudioChannels, samplingRate: SamplingRate)(state: Unit): Unit = {
    println("Time since last frame " + (System.currentTimeMillis() - lastReceivedFrame))
    lastReceivedFrame = System.currentTimeMillis()
    State.callManager.get(callNumber).foreach(_.onAudioFrame(pcm, channels, samplingRate))
  }
}