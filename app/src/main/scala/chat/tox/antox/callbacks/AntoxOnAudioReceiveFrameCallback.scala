package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.CallNumber
import im.tox.tox4j.av.data.{AudioChannels, SamplingRate}

class AntoxOnAudioReceiveFrameCallback(private var ctx: Context) {

  def audioReceiveFrame(callNumber: CallNumber, pcm: Array[Short], channels: AudioChannels, samplingRate: SamplingRate)(state: Unit): Unit = {
    State.callManager.get(callNumber).foreach(_.onAudioFrame(pcm, channels, samplingRate))
  }
}