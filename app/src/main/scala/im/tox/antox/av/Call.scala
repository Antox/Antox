package im.tox.antox.av

import im.tox.tox4j.av.enums.ToxCallState

class Call(val friendNumber: Integer,
           var state: Option[ToxCallState],
           val audioBitRate: Int,
           val videoBitRate: Int) {

  val audioCapture: AudioCapture = new AudioCapture()

  val playAudio = new PlayAudio()

  def start(): Unit = {
    audioCapture.startCapture(audioBitRate)
  }

  def mute(): Unit = {
    audioCapture.stopCapture()
  }

  def end(): Unit = {
    audioCapture.stopCapture()
  }
}
