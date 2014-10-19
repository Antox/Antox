package im.tox.antox.utils

import android.media.{MediaRecorder, AudioFormat, AudioRecord}
import android.os.AsyncTask
import android.util.Log
import im.tox.antox.tox.ToxSingleton
import im.tox.jtoxcore.ToxCodecSettings
import rx.lang.scala.Observable

object CaptureAudio {

  val TAG = "im.tox.antox.utils.CaptureAudio"

  var bufferSizeBytes: Int = _
  private val sampleRates: Array[Int] = Array(8000, 11025, 22050, 44100)

  def makeObservable(callID: Integer, codecSettings: ToxCodecSettings): Observable[Boolean] = {
    Observable[Boolean](subscriber => {
      val mAudioRecord = findAudioRecord()
      val rtpPayloadSize = 65535
      mAudioRecord match {
        case Some(audioRecord) => {
          audioRecord.startRecording()

          while (!subscriber.isUnsubscribed) {
            try {
              val buffer = Array.ofDim[Short](CaptureAudio.bufferSizeBytes)
              audioRecord.read(buffer, 0, CaptureAudio.bufferSizeBytes)
              val intBuffer = buffer.map(x => x: Int)
              val preparedBuffer = ToxSingleton.jTox.avPrepareAudioFrame(callID,
                rtpPayloadSize, intBuffer, codecSettings.audio_frame_duration)
              ToxSingleton.jTox.avSendAudio(callID, preparedBuffer)
              Log.d("Mic", "Sending audio to:" + callID)
            } catch {
              case e: Exception => 
                e.printStackTrace
                subscriber.onError(e)
            }
          }

          audioRecord.stop()
          audioRecord.release()
        }
        case None => Log.d(TAG, "Audio record: None!")
      }
      subscriber.onCompleted()
    })
  }

  private def findAudioRecord(): Option[AudioRecord] = {
    try {
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT
      val channelConfig = AudioFormat.CHANNEL_IN_MONO
      val rate = 48000
      Log.d("CaptureAudio", "Attempting rate " + rate + "Hz, bits: " + audioFormat +
        ", channel: " +
        channelConfig)
      val bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat)
      if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
        val recorder = new AudioRecord(0, rate, channelConfig, audioFormat,
          bufferSize)
        if (recorder.getState == AudioRecord.STATE_INITIALIZED) {
          CaptureAudio.bufferSizeBytes = bufferSize
          return Some(recorder)
        } else {
          None
        }
      } else {
        None
      }
    } catch {
      case e: Exception => 
        e.printStackTrace
        None
    }
  }
}
