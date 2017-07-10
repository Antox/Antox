package chat.tox.antox.av

import android.media.{AudioFormat, AudioManager, AudioTrack}
import android.util.Log

import scala.collection.mutable

class AudioPlayer(_sampleRate: Int, _channels: Int, minBufferLength: Int) extends AudioDevice(_sampleRate, _channels) {

  var active = false
  var dirty = true

  private var mAudioTrack: Option[AudioTrack] = None
  private val audioBuffer = new mutable.Queue[(Array[Short], Int, Int)]

  def recreate(): Unit = {
    require(channels <= 2 && channels > 0, "channels must be either 1 or 2")

    //currently only support 2 channels
    val channelConfig =
      if (channels == 1) {
        AudioFormat.CHANNEL_OUT_MONO
      } else {
        AudioFormat.CHANNEL_OUT_STEREO
      }

    val bufferSize = sampleRate * channels * 2
    mAudioTrack = Some(new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelConfig,
      AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM))
    mAudioTrack.foreach(_.play())
    dirty = false
    println("recreating audio whatever")
  }

  def bufferAudioFrame(data: Array[Short], channels: Int, sampleRate: Int): Unit = {
    audioBuffer.enqueue((data, channels, sampleRate))
  }

  //returns the duration in milliseconds of the playback
  def playAudioFrame(): Int = {
    if (audioBuffer.length > minBufferLength) {
      //update sample rate and channels if they've changed
      val (data, newChannels, newSampleRate) = audioBuffer.dequeue()
      if (channels != newChannels) {
        channels = newChannels
      }

      if (sampleRate != newSampleRate) {
        sampleRate = newSampleRate
      }

      if (dirty) recreate()

      try {
        // mAudioTrack shouldn't ever be None here. fail fast with .get
        mAudioTrack.get.write(data, 0, data.length)
      } catch {
        case e: Exception => Log.e("AudioPlayback", e.getMessage)
      }
      (data.length / sampleRate) * 1000
    } else {
      0
    }
  }

  def start(): Unit =
    if (!active) {
      active = true
      new Thread(new Runnable {
        override def run(): Unit = {
          while (active) {
            val sleepTime = playAudioFrame()
            Thread.sleep(sleepTime)
          }
        }
      }).start()
    }

  def stop(): Unit = {
    active = false
  }

  def cleanUp(): Unit = {
    stop()

    mAudioTrack.foreach(audioTrack => {
      audioTrack.flush()
      audioTrack.release()
    })
  }
}
