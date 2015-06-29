package im.tox.antox.av

import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AudioCapture
import im.tox.tox4j.av.enums.{ToxCallControl, ToxCallState}

class Call(val friendNumber: Int) {

  var active = false
  var state: Set[ToxCallState] = Set(ToxCallState.FINISHED)

  val sampleRate = 48000 //in Hz
  val audioLength = 60 //in ms
  val channels = 1

  def frameSize = (sampleRate * audioLength) / 1000

  var initialAudioBitRate = 0
  var initialVideoBitrate = 0

  var _audioBitRate: Int = 0
  var _videoBitRate: Int = 0

  def sendingAudio = audioBitRate > 0
  def sendingVideo = videoBitRate > 0

  def receivingAudio = state.contains(ToxCallState.SENDING_A)
  def receivingVideo = state.contains(ToxCallState.SENDING_V)

  val audioCapture: AudioCapture = new AudioCapture()

  val playAudio = new PlayAudio()

  def startCall(audioBitRate: Int, videoBitRate: Int): Unit = {
    ToxSingleton.toxAv.call(friendNumber, audioBitRate, videoBitRate)

    initialAudioBitRate = audioBitRate
    initialVideoBitrate = videoBitRate

    this.audioBitRate = audioBitRate
    this.videoBitRate = videoBitRate
  }

  def answerCall(audioBitRate: Int, videoBitRate: Int, receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    ToxSingleton.toxAv.answer(friendNumber, audioBitRate, videoBitRate)

    initialAudioBitRate = audioBitRate
    initialVideoBitrate = videoBitRate

    this.audioBitRate = audioBitRate
    this.videoBitRate = videoBitRate

    callStarted(audioBitRate, videoBitRate)
  }

  def onAnswered(): Unit = {
    callStarted(audioBitRate, videoBitRate)
  }

  private def callStarted(audioBitRate: Int, videoBitRate: Int): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        audioCapture.startCapture(sampleRate, channels)

        while (active) {
          val start = System.nanoTime()
          if (sendingAudio) {
            ToxSingleton.toxAv.audioSendFrame(friendNumber,
              audioCapture.readAudio(frameSize),
              frameSize, channels, sampleRate)
          }

          val timeTaken = System.nanoTime() - start
          if (timeTaken < audioLength)
            Thread.sleep((audioLength - (timeTaken / 10^6)) - 1)
        }

        audioCapture.stopCapture()
      }
    }).start()

    active = true
  }

  def onAudioFrame(pcm: Array[Short]): Unit = {
    playAudio.playAudioFrame(pcm)
  }

  def muteSelfAudio(): Unit = {
    audioBitRate = 0
    audioCapture.stopCapture()
  }

  def unmuteSelfAudio(): Unit = {
    audioBitRate = initialAudioBitRate
    audioCapture.startCapture(sampleRate, channels)
  }

  def hideSelfVideo(): Unit = {
    videoBitRate = 0
  }

  def showSelfVideo(): Unit = {
    videoBitRate = initialVideoBitrate
  }

  def muteSpeaker(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.MUTE_AUDIO)
  }

  def unmuteSpeaker(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.UNMUTE_AUDIO)
  }

  def hideVideo(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.HIDE_VIDEO)
  }

  def showVideo(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.SHOW_VIDEO)
  }

  def end(): Unit = {
    audioCapture.stopCapture()
    cleanUp()
    active = false
  }

  private def cleanUp(): Unit = {
    audioCapture.cleanUp()
  }

  //getters
  def audioBitRate = _audioBitRate
  def videoBitRate = _videoBitRate

  //setters
  def audioBitRate_= (newAudioBitRate: Int): Unit = {
    _audioBitRate = newAudioBitRate
    //ToxSingleton.toxAv.audioBitRateSet(friendNumber, newAudioBitRate, force = true)
  }

  def videoBitRate_= (newVideoBitRate: Int): Unit = {
    _videoBitRate = newVideoBitRate
    //ToxSingleton.toxAv.videoBitRateSet(friendNumber, newVideoBitRate, force = true)
  }
}
