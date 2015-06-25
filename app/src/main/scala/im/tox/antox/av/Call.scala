package im.tox.antox.av

import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AudioCapture
import im.tox.tox4j.av.enums.ToxCallState

class Call(val friendNumber: Int) {

  var active = false
  var state: Set[ToxCallState] = Set(ToxCallState.FINISHED)

  var _audioBitRate: Int = 0
  var _videoBitRate: Int = 0

  def sendingAudio = audioBitRate > 0
  def sendingVideo = videoBitRate > 0

  var receivingAudio = false
  var receivingVideo = false

  val audioCapture: AudioCapture = new AudioCapture()

  val playAudio = new PlayAudio()

  def startCall(audioBitRate: Int, videoBitRate: Int): Unit = {
    ToxSingleton.toxAv.call(friendNumber, audioBitRate, videoBitRate)
  }

  def answerCall(audioBitRate: Int, videoBitRate: Int, receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    ToxSingleton.toxAv.answer(friendNumber, audioBitRate, videoBitRate)
    callStarted(audioBitRate, videoBitRate)

    this.receivingAudio = receivingAudio
    this.receivingVideo = receivingVideo
  }

  private def callStarted(audioBitRate: Int, videoBitRate: Int): Unit = {
    this.audioBitRate = audioBitRate
    this.videoBitRate = videoBitRate

    active = true
  }

  def onAnswered(): Unit = {
    if (!sendingAudio) audioCapture.startCapture(audioBitRate)
  }

  def onAudioFrame(pcm: Array[Short]): Unit = {
    playAudio.playAudioFrame(pcm)
  }

  def muteMic(): Unit = {
    audioBitRate = 0
    audioCapture.stopCapture()
  }

  def muteVideo(): Unit = {
    videoBitRate = 0
  }

  def endCall(): Unit = {
    audioCapture.stopCapture()
    active = false
  }

  def cleanUp(): Unit = {
    audioCapture.cleanUp()
  }

  //getters
  def audioBitRate = _audioBitRate
  def videoBitRate = _videoBitRate

  //setters
  def audioBitRate_= (newAudioBitRate: Int): Unit = {
    _audioBitRate = newAudioBitRate
    ToxSingleton.toxAv.audioBitRateSet(friendNumber, newAudioBitRate, force = true)
  }

  def videoBitRate_= (newVideoBitRate: Int): Unit = {
    _videoBitRate = newVideoBitRate
    ToxSingleton.toxAv.videoBitRateSet(friendNumber, newVideoBitRate, force = true)
  }
}
