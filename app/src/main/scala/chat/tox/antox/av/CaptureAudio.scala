package chat.tox.antox.av

import android.media.{AudioFormat, AudioRecord}
import chat.tox.antox.utils.AntoxLog
import rx.lang.scala.Observable

object CaptureAudio {

  var bufferSizeBytes: Int = _

  def sendAudio(callID: Int, audioRecord: AudioRecord, audioBitRate: Int): Unit = {
    /* var channels = 1
    var frameSize = (codecSettings.audio_frame_duration * codecSettings.audio_sample_rate) / 1000 * channels

    val buffer = Array.ofDim[Short](CaptureAudio.bufferSizeBytes)
    audioRecord.read(buffer, 0, CaptureAudio.bufferSizeBytes)
    val intBuffer = buffer.map(x => x: Int)
    val preparedBuffer = ToxSingleton.tox.avPrepareAudioFrame(callID,
      frameSize * 2, intBuffer, frameSize)
    ToxSingleton.tox.avSendAudio(callID, preparedBuffer) */
  }

  def makeObservable(callID: Integer, audioBitRate: Int): Observable[Boolean] = {
    Observable[Boolean](subscriber => {
     /* val mAudioRecord = findAudioRecord()
      mAudioRecord match {
        case Some(audioRecord) => {
          audioRecord.startRecording()

          while (!subscriber.isUnsubscribed) {
            try {
              sendAudio(callID, audioRecord, codecSettings)
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
      } */
      subscriber.onCompleted()
    })
  }

  private def findAudioRecord(): Option[AudioRecord] = {
    try {
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT
      val channelConfig = AudioFormat.CHANNEL_IN_MONO
      val rate = 48000
      AntoxLog.debug(s"Attempting rate $rate Hz, bits: $audioFormat , channel: $channelConfig")
      val bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat)
      if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
        val recorder = new AudioRecord(0, rate, channelConfig, audioFormat,
          bufferSize)
        if (recorder.getState == AudioRecord.STATE_INITIALIZED) {
          CaptureAudio.bufferSizeBytes = bufferSize
          Some(recorder)
        } else {
          None
        }
      } else {
        None
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }
  }
}
