package im.tox.antox.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.util.Log
import im.tox.antox.tox.ToxSingleton
//remove if not needed
import scala.collection.JavaConversions._

class CaptureAudio extends AsyncTask[String, Void, Void] {

  protected override def doInBackground(params: String*): Void = {
    val bufferSizeBytes = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    val audioRecord = new AudioRecord(1, 48000, AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT, bufferSizeBytes)
    audioRecord.startRecording()
    while (true) {
      if (isCancelled) //break
        try {
          val buffer = Array.ofDim[Byte](bufferSizeBytes)
          val read = audioRecord.read(buffer, 0, bufferSizeBytes)
          ToxSingleton.jTox.avSendAudio(java.lang.Integer.parseInt(params(0)), buffer)
          Log.d("Mic", "Sending audio to:" + params(0))
        } catch {
          case e: Exception =>
        }
    }
    audioRecord.stop()
    audioRecord.release()
    null
  }
}
