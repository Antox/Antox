package im.tox.antox.callbacks

import android.content.Context
import im.tox.antox.data.State
import im.tox.antox.wrapper.AntoxFriend

class AntoxOnAudioDataCallback(private var ctx: Context) {

  def execute(callID: Int, data: Array[Byte]) {
    State.calls.get(callID).foreach(call => call.playAudio.playAudioBuffer(data))
  }

}
