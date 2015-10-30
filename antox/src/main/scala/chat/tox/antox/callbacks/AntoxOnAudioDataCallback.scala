package chat.tox.antox.callbacks

import android.content.Context
import chat.tox.antox.data.State

class AntoxOnAudioDataCallback(private var ctx: Context) {

  def execute(callID: Int, data: Array[Byte]) {
    State.calls.get(callID).foreach(call => call.playAudio.playAudioBuffer(data))
  }

}
