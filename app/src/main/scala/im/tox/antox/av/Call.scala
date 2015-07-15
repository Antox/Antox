package im.tox.antox.av

import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AudioCapture
import im.tox.tox4j.av.enums.{ToxCallControl, ToxCallState}
import im.tox.tox4j.av.exceptions.ToxAvSendFrameException
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.subjects.BehaviorSubject

class Call(val friendNumber: Int) {

  private var friendState: Set[ToxCallState] = Set()
  val friendStateSubject = BehaviorSubject[Set[ToxCallState]](friendState)

  private var selfState = SelfCallState.DEFAULT

  //only for outgoing audio
  private val sampleRate = 48000 //in Hz
  private val audioLength = 20 //in ms
  private val channels = 2

  val ringing = BehaviorSubject[Boolean](false)
  var incoming = false

  var startTime: Long = 0
  def duration = System.currentTimeMillis() - startTime //in seconds

  def active = !friendState.contains(ToxCallState.FINISHED)
  def onHold = friendState.isEmpty

  val audioCapture: AudioCapture = new AudioCapture(sampleRate, channels)
  val audioPlayer = new AudioPlayer(sampleRate, channels)

  private def frameSize = (sampleRate * audioLength) / 1000

  friendStateSubject.subscribe(_ => {
    if (active) {
      ringing.onNext(false)
    }
  })

  def startCall(audioBitRate: Int, videoBitRate: Int): Unit = {
    ToxSingleton.toxAv.call(friendNumber, audioBitRate, videoBitRate)
    selfState = selfState.copy(audioBitRate = audioBitRate, videoBitRate = videoBitRate)
    incoming = false
    ringing.onNext(true)
  }

  def answerCall(receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    ToxSingleton.toxAv.answer(friendNumber, selfState.audioBitRate, selfState.videoBitRate)
    callStarted(selfState.audioBitRate, selfState.videoBitRate)
    ringing.onNext(false)
  }

  def onIncoming(receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    incoming = true
    ringing.onNext(true)
    selfState = selfState.copy(receivingAudio = receivingAudio, receivingVideo = receivingVideo)
  }

  def updateFriendState(state: Set[ToxCallState]): Unit = {
    friendState = state
    friendStateSubject.onNext(friendState)
  }

  private def callStarted(audioBitRate: Int, videoBitRate: Int): Unit = {
    startTime = System.currentTimeMillis()

    new Thread(new Runnable {
      override def run(): Unit = {
        audioCapture.start()

        while (active) {
          val start = System.nanoTime()
          if (selfState.sendingAudio) {
            try {
              ToxSingleton.toxAv.audioSendFrame(friendNumber,
                audioCapture.readAudio(frameSize, channels),
                frameSize, channels, sampleRate)
            } catch {
              case e: ToxException[_] =>
                end(error = true)
            }
          }

          val timeTaken = System.nanoTime() - start
          if (timeTaken < audioLength)
            Thread.sleep(audioLength - (timeTaken / 10^6))
        }
      }
    }).start()

    audioPlayer.start()
  }

  def onAudioFrame(pcm: Array[Short], channels: Int, sampleRate: Int): Unit = {
    audioPlayer.bufferAudioFrame(pcm, channels, sampleRate)
  }

  def muteSelfAudio(): Unit = {
    selfState = selfState.copy(audioMuted = true)
    ToxSingleton.toxAv.audioBitRateSet(friendNumber, 0, force = true)
    audioCapture.stop()
  }

  def unmuteSelfAudio(): Unit = {
    selfState = selfState.copy(audioMuted = false)
    audioCapture.start()
  }

  def hideSelfVideo(): Unit = {
    selfState = selfState.copy(videoHidden = true)
  }

  def showSelfVideo(): Unit = {
    selfState = selfState.copy(videoHidden = false)
    //TODO
  }

  def muteFriendAudio(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.MUTE_AUDIO)
  }

  def unmuteFriendAudio(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.UNMUTE_AUDIO)
  }

  def hideFriendVideo(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.HIDE_VIDEO)
  }

  def showFriendVideo(): Unit = {
    ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.SHOW_VIDEO)
  }

  def end(error: Boolean = false): Unit = {
    // only send a call control if the call wasn't ended unexpectedly
    if (!error) {
      ToxSingleton.toxAv.callControl(friendNumber, ToxCallControl.CANCEL)
    }

    audioCapture.stop()
    cleanUp()

    friendState = Set()
    selfState = SelfCallState.DEFAULT
  }

  private def cleanUp(): Unit = {
    audioPlayer.cleanUp()
    audioCapture.cleanUp()
  }
}
