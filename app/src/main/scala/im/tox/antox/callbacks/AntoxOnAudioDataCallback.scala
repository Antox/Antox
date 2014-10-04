package im.tox.antox.callbacks

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.callbacks.OnAudioDataCallback
//remove if not needed
import scala.collection.JavaConversions._

class AntoxOnAudioDataCallback(private var ctx: Context) extends OnAudioDataCallback[AntoxFriend] {

  def execute(callID: Int, data: Array[Byte]) {
    Log.d("OnAudioDataCallback", "Received callback from: " + callID)
    try {
      val audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 48000, AudioFormat.CHANNEL_OUT_DEFAULT,
        AudioFormat.ENCODING_PCM_16BIT, data.length, AudioTrack.MODE_STREAM)
      audioTrack.play()
      audioTrack.write(data, 0, data.length)
      audioTrack.stop()
      audioTrack.release()
    } catch {
      case e: Exception => Log.e("AudioPlayback", e.getMessage)
    }
  }
}
