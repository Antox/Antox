package chat.tox.antox.av

import android.media.{AudioFormat, AudioRecord}
import chat.tox.antox.utils.AntoxLog
import org.scaloid.common.LoggerTag

class AudioCapture(_sampleRate: Int, _channels: Int) extends AudioDevice(_sampleRate, _channels) {

  val TAG = LoggerTag(getClass.getSimpleName)

  var active: Boolean = false
  var dirty = true

  var bufferSizeBytes: Int = _
  var mAudioRecord: Option[AudioRecord] = None
  var callAudioEffects: Option[CallAudioEffects] = None

  val VOICE_COMMUNICATION: Int = 7 // audio source

  def recreate(): Unit = {
    require(channels <= 2 && channels > 0, "channels must be either 1 or 2")

    AntoxLog.debug("AudioCapture recreated.", TAG)

    mAudioRecord = findAudioRecord(sampleRate, channels)
    mAudioRecord match {
      case Some(audioRecord) => audioRecord.startRecording()
      case None => AntoxLog.debug("Could not get AudioRecord.")
    }

    dirty = false
  }

  def start(): Unit = {
    AntoxLog.debug("AudioCapture started.", TAG)

    recreate()

    active = true
  }

  def readAudio(frames: Int, readChannels: Int): Array[Short] = {
    if (this.channels != readChannels) {
      channels = readChannels
    }

    if (dirty) {
      recreate()
    }

    val audio = Array.ofDim[Short](frames * channels)
    mAudioRecord.foreach(ar => {
      ar.read(audio, 0, audio.length)
    })

    audio
  }

  def stop(): Unit = {
    AntoxLog.debug("AudioCapture stopped.", TAG)
    mAudioRecord.foreach(audioRecord => {
      if (audioRecord.getState == AudioRecord.STATE_INITIALIZED && active) {
        audioRecord.stop()
      }
    })
    active = false
  }

  def cleanUp(): Unit = {
    mAudioRecord.foreach(_.release())
    callAudioEffects.foreach(_.cleanUp())
  }

  private def findAudioRecord(sampleRate: Int, channels: Int): Option[AudioRecord] = {
    try {
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT

      //currently only support up to 2 channels
      val channelConfig =
        if (channels == 1) {
          AudioFormat.CHANNEL_IN_MONO
        } else {
          AudioFormat.CHANNEL_IN_STEREO
        }

      AntoxLog.debug(s"Attempting rate $sampleRate Hz, bits: $audioFormat, channel: $channels", TAG)

      val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
      if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {

        val recorder = new AudioRecord(VOICE_COMMUNICATION, sampleRate, channelConfig, audioFormat, bufferSize * 10)
        if (recorder.getState == AudioRecord.STATE_INITIALIZED) {
          bufferSizeBytes = bufferSize

          callAudioEffects.foreach(_.cleanUp())
          callAudioEffects = Some(new CallAudioEffects(recorder))
          callAudioEffects.foreach(_.enable())

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
