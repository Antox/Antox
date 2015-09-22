package chat.tox.antox.av

import android.media.{AudioFormat, AudioManager, AudioTrack}
import android.util.Log

class PlayAudio{
  private val TAG = "chat.tox.antox.av.PlayAudio"

  var audioTrack: AudioTrack = null

  def playAudioBuffer(data: Array[Byte]): Unit ={
    try {
      if (audioTrack == null || audioTrack.getState == AudioTrack.STATE_UNINITIALIZED){
        val defaultBitrate = 48000
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, defaultBitrate, AudioFormat.CHANNEL_OUT_DEFAULT,
          AudioFormat.ENCODING_PCM_16BIT, data.length, AudioTrack.MODE_STREAM) //TODO: change this back to a phone call
      }
          audioTrack.play ()
          audioTrack.write (data, 0, data.length)
          audioTrack.stop ()
    } catch {
      case e: Exception => Log.e(TAG, "exception", e)
    }
  }

  def cleanUp(): Unit ={
    audioTrack.flush()
    audioTrack.release()
  }
}
