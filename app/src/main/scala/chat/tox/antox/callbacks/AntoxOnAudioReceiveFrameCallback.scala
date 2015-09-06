package chat.tox.antox.callbacks

import android.content.Context
import im.tox.tox4j.av.callbacks.AudioReceiveFrameCallback

class AntoxOnAudioReceiveFrameCallback(private var ctx: Context) extends AudioReceiveFrameCallback[Unit] {

  var lastReceivedFrame: Long = 0
  override def audioReceiveFrame(friendNumber: Int, pcm: Array[Short], channels: Int, sampleRate: Int)(state: Unit): Unit = {
    println("Time since last frame " + (System.currentTimeMillis() - lastReceivedFrame))
    lastReceivedFrame = System.currentTimeMillis()
    //ToxSingleton.getAntoxFriend(friendNumber).get.call.onAudioFrame(pcm, channels, sampleRate)
  }
}
