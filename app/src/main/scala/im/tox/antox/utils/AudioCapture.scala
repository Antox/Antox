package im.tox.antox.utils

import android.media.{AudioFormat, AudioRecord}
import android.util.Log
import im.tox.antox.exceptions.AvDeviceNotFoundException
import im.tox.antox.tox.ToxSingleton
import rx.lang.scala.Observable

import scala.None

class AudioCapture(var _sampleRate: Int, var _channels: Int) {

  val TAG = "im.tox.antox.utils.CaptureAudio"

  var bufferSizeBytes: Int = _
  var mAudioRecord: Option[AudioRecord] = None

  var capturing: Boolean = false

  // if the track is dirty it will be recreated on the next playback
  private var dirty = true

  def recreate(): Unit = {
    require(channels <= 2 && channels > 0, "channels must be either 1 or 2")
  }

  def startCapture(sampleRate: Int, channels: Int): Unit = {
    if (capturing) stopCapture() //if already capturing stop and reset the audio record with a (possibly new)

    mAudioRecord = findAudioRecord(sampleRate, channels)
    mAudioRecord match {
      case Some(audioRecord) => audioRecord.startRecording()
      case None => throw AvDeviceNotFoundException("Could not get AudioRecord.")
    }

    capturing = true
  }

  def readAudio(frames: Int, channels: Int): Array[Short] = {
    val audio = Array.ofDim[Short](frames * channels)
    mAudioRecord.foreach(ar => ar.read(audio, 0, frames * channels))
    audio
  }

  def stopCapture(): Unit = {
    mAudioRecord.foreach(_.stop())
    capturing = false
  }

  def cleanUp(): Unit = {
    stopCapture()
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
        channelConfig)
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

  //getters
  def sampleRate = _sampleRate
  def channels = _channels

  //setters
  def sampleRate_= (sampleRate: Int): Unit = {
    _sampleRate = sampleRate
    dirty = true
  }

  def channels_= (channels: Int): Unit = {
    _channels = channels
    dirty = true
  }
}
