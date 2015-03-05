package im.tox.antox.av

import android.media.{AudioFormat, AudioManager, AudioTrack}
import android.util.Log

class PlayAudio{
  private val TAG = "im.tox.antox.av.PlayAudio"

  var audioTrack: AudioTrack = null

  def playAudioBuffer(data: Array[Byte]): Unit ={
    try {
      if (audioTrack == null || audioTrack.getState == AudioTrack.STATE_UNINITIALIZED){
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_DEFAULT,
          AudioFormat.ENCODING_PCM_16BIT, data.length, AudioTrack.MODE_STREAM) //TODO: change this back to a phone call
      }
          audioTrack.play ()
          audioTrack.write (data, 0, data.length)
          audioTrack.stop ()
    } catch {
      case e: Exception => Log.e("AudioPlayback", e.getMessage)
    }
  }

  def cleanUp(): Unit ={
    audioTrack.flush()
    audioTrack.release()
  }
}
