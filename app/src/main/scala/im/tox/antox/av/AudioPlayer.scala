package im.tox.antox.av

import android.media.{AudioFormat, AudioManager, AudioTrack}
import android.util.Log
import org.apache.commons.collections4.queue.CircularFifoQueue

class AudioPlayer(var _sampleRate: Int, var _channels: Int, bufferSize: Int = 8) {

  private var running = false

  private var mAudioTrack: Option[AudioTrack] = None
  private val audioBuffer = new CircularFifoQueue[Array[Short]](bufferSize)

  // if the track is dirty it will be recreated on the next playback
  private var dirty = true

  def recreateAudioTrack(): Unit = {
    require(channels <= 2 && channels > 0, "channels must be either 1 or 2")

    //currently only support 2 channels
    val channelConfig =
      if(channels == 1) {
        AudioFormat.CHANNEL_OUT_MONO
      } else {
        AudioFormat.CHANNEL_OUT_STEREO
      }

    val bufferSize = sampleRate * channels * 2
    mAudioTrack = Some(new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
      AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM)) //TODO: change this back to a phone call
    mAudioTrack.foreach(_.play())
    dirty = false
    println("recreating audio whatever")
  }

  def bufferAudioFrame(data: Array[Short]): Unit ={
    audioBuffer.add(data)
  }

  //returns the duration in milliseconds of the playback
  def playAudioFrame(): Int = {
    if (dirty) recreateAudioTrack()

    val data = audioBuffer.poll()

    if (data != null) {
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

  def start(): Unit = {
    if (running) return

    running = true
    new Thread(new Runnable {
      override def run(): Unit = {
        while (running) {
          val sleepTime = playAudioFrame()
          Thread.sleep(sleepTime)
        }
      }
    }).start()
  }

  def stop(): Unit = {
    running = false
  }

  def cleanUp(): Unit = {
    stop()

    mAudioTrack.foreach(audioTrack => {
      audioTrack.flush()
      audioTrack.release()
    })
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
