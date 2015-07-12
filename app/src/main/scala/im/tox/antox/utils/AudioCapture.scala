package im.tox.antox.utils

import android.media.{AudioFormat, AudioRecord}
import android.util.Log
import im.tox.antox.av.AudioDevice
import im.tox.antox.exceptions.AvDeviceNotFoundException
import im.tox.antox.tox.ToxSingleton
import rx.lang.scala.Observable

import scala.None

class AudioCapture(_sampleRate: Int, _channels: Int) extends AudioDevice(_sampleRate, _channels) {

  val TAG = "im.tox.antox.utils.CaptureAudio"

  var active: Boolean = false
  var dirty = true

  var bufferSizeBytes: Int = _
  var mAudioRecord: Option[AudioRecord] = None

  def recreate(): Unit = {
    require(channels <= 2 && channels > 0, "channels must be either 1 or 2")

    mAudioRecord = findAudioRecord(sampleRate, channels)
    mAudioRecord match {
      case Some(audioRecord) => audioRecord.startRecording()
      case None => throw AvDeviceNotFoundException("Could not get AudioRecord.")
    }

    dirty = false
  }

  def start(): Unit = {
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
    mAudioRecord.foreach(ar => ar.read(audio, 0, frames * channels))
    audio
  }

  def stop(): Unit = {
    mAudioRecord.foreach(audioRecord => {
      if (audioRecord.getState == AudioRecord.STATE_INITIALIZED) {
        audioRecord.stop()
      }
    })
    active = false
  }

  def cleanUp(): Unit = {
    stop()
    mAudioRecord.foreach(_.release())
  }

  private def findAudioRecord(sampleRate: Int, channels: Int): Option[AudioRecord] = {
    try {
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT

      //currently only support up to 2 channels
      val channelConfig =
        if(channels == 1) {
          AudioFormat.CHANNEL_IN_MONO
        } else {
          AudioFormat.CHANNEL_IN_STEREO
        }

      Log.d("CaptureAudio", "Attempting rate " + sampleRate + "Hz, bits: " + audioFormat +
        ", channel: " +
        channels)
      val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
      if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
        val recorder = new AudioRecord(0, sampleRate, channelConfig, audioFormat,
          bufferSize)
        if (recorder.getState == AudioRecord.STATE_INITIALIZED) {
          bufferSizeBytes = bufferSize
          return Some(recorder)
        }
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }

    None
  }


}
