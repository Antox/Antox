package im.tox.antox.utils

import android.media.{AudioFormat, AudioRecord}
import android.util.Log
import im.tox.antox.exceptions.AvDeviceNotFoundException
import im.tox.antox.tox.ToxSingleton
import rx.lang.scala.Observable

import scala.None

class AudioCapture {

  val TAG = "im.tox.antox.utils.CaptureAudio"

  var bufferSizeBytes: Int = _
  var audioRecord: Option[AudioRecord] = None

  var capturing: Boolean = false

  def startCapture(bitrate: Int): Unit = {
    if (capturing) stopCapture() //if already capturing stop and reset the audio record with a (possibly new) bitrate
    audioRecord = findAudioRecord(bitrate)
    audioRecord match {
      case Some(audioRecord) => audioRecord.startRecording()
      case None => throw AvDeviceNotFoundException("Could not get AudioRecord.")
    }

    capturing = true
  }

  def getAudio(): Unit = {
    if (!capturing) throw new IllegalStateException("Can't stop capturing when already capturing.")
  }

  def stopCapture(): Unit = {
    if (!capturing) throw new IllegalStateException("Can't stop capturing when already capturing.")
    audioRecord match {
      case Some(audioRecord) =>
        audioRecord.stop()
        audioRecord.release()
      case None => throw AvDeviceNotFoundException("Could not get AudioRecord.")
    }

    capturing = false
  }

  private def findAudioRecord(bitrate: Int): Option[AudioRecord] = {
    try {
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT
      val channelConfig = AudioFormat.CHANNEL_IN_MONO
      Log.d("CaptureAudio", "Attempting rate " + bitrate + "Hz, bits: " + audioFormat +
        ", channel: " +
        channelConfig)
      val bufferSize = AudioRecord.getMinBufferSize(bitrate, channelConfig, audioFormat)
      if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
        val recorder = new AudioRecord(0, bitrate, channelConfig, audioFormat,
          bufferSize)
        if (recorder.getState == AudioRecord.STATE_INITIALIZED) {
          bufferSizeBytes = bufferSize
          return Some(recorder)
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
