package chat.tox.antox.av

import java.util.concurrent.TimeUnit

import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{AntoxLog, AudioCapture}
import chat.tox.antox.wrapper.{CallNumber, ContactKey}
import im.tox.tox4j.av._
import im.tox.tox4j.av.enums.{ToxavCallControl, ToxavFriendCallState}
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.subjects.BehaviorSubject

import scala.concurrent.duration.Duration

class Call(val callNumber: CallNumber, val contactKey: ContactKey) {

  private var friendState: Set[ToxavFriendCallState] = Set()
  val friendStateSubject = BehaviorSubject[Set[ToxavFriendCallState]](friendState)

  private var selfState = SelfCallState.DEFAULT

  //only for outgoing audio
  private val samplingRate = SamplingRate.Rate48k //in Hz
  private val audioLength = AudioLength.Length20 //in microseconds
  private val channels = AudioChannels.Stereo

  val defaultRingTime = Duration(30, TimeUnit.SECONDS)
  val ringing = BehaviorSubject[Boolean](false)
  var incoming = false

  var startTime: Long = 0
  def duration: Long = System.currentTimeMillis() - startTime //in milliseconds

  /**
   * Describes a state in which the call is not FINISHED or ERROR.
   * When the call is on hold or ringing (not yet answered) this will return true.
   */
  def active: Boolean = {
    isActive(friendState)
  }

  def isActive(state: Set[ToxavFriendCallState]): Boolean = {
    !state.contains(ToxavFriendCallState.FINISHED) && !state.contains(ToxavFriendCallState.ERROR)
  }

  def onHold: Boolean = friendState.isEmpty

  val audioCapture: AudioCapture = new AudioCapture(samplingRate.value, channels.value)
  val audioPlayer = new AudioPlayer(samplingRate.value, channels.value)

  private def frameSize = SampleCount(audioLength, samplingRate)

  def logCallEvent(event: String): Unit = AntoxLog.debug(s"Call $callNumber belonging to $contactKey $event")
  
  def startCall(sendingAudio: Boolean, sendingVideo: Boolean): Unit = {
    logCallEvent(s"started sending audio:$sendingAudio and video:$sendingVideo")
    ToxSingleton.toxAv.call(
      callNumber.value,
      if (sendingAudio) selfState.audioBitRate else BitRate.Disabled,
      if (sendingVideo) selfState.videoBitRate else BitRate.Disabled)

    endAfterTime(defaultRingTime)

    incoming = false
    ringing.onNext(true)
  }

  def answerCall(receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    logCallEvent(s"answered receiving audio:$receivingAudio and video:$receivingVideo")

    ToxSingleton.toxAv.answer(callNumber.value, selfState.audioBitRate, selfState.videoBitRate)
    callStarted()
    ringing.onNext(false)
  }

  def onIncoming(receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    logCallEvent(s"incoming receiving audio:$receivingAudio and video:$receivingVideo")

    endAfterTime(defaultRingTime)

    incoming = true
    ringing.onNext(true)
    selfState = selfState.copy(receivingAudio = receivingAudio, receivingVideo = receivingVideo)
  }
  
  def endAfterTime(ringTime: Duration): Unit = {
    //end the call after `ringTime`
    new Thread(new Runnable {
      override def run(): Unit = {
        Thread.sleep(ringTime.toMillis)
        if (active && ringing.getValue) end(false)
      }
    }).start()
  }

  def updateFriendState(state: Set[ToxavFriendCallState]): Unit = {
    logCallEvent(s"friend call state updated to $state")

    if (friendState.isEmpty && isActive(state) && !incoming) {
      callStarted()
      ringing.onNext(false)
    }

    friendState = state
    friendStateSubject.onNext(friendState)
  }

  private def callStarted(): Unit = {
    startTime = System.currentTimeMillis()

    logCallEvent(event = s"started at time $startTime")
    
    new Thread(new Runnable {
      override def run(): Unit = {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

        logCallEvent(s"audio encoded thread started")

        audioCapture.start()

        while (active) {
          val start = System.currentTimeMillis()
          if (selfState.sendingAudio) {
            try {
              ToxSingleton.toxAv.audioSendFrame(callNumber.value,
                audioCapture.readAudio(frameSize.value, channels.value),
                frameSize, channels, samplingRate)
            } catch {
              case e: ToxException[_] =>
                end(error = true)
            }
          }

          val timeTaken = System.currentTimeMillis() - start
          if (timeTaken < audioLength.value.toMillis)
            Thread.sleep(audioLength.value.toMillis - timeTaken)
        }

        logCallEvent(s"audio encoded thread stopped")
      }
    }, "AudioSendThread").start()

    audioPlayer.start()
  }

  def onAudioFrame(pcm: Array[Short], channels: AudioChannels, samplingRate: SamplingRate): Unit = {
    audioPlayer.bufferAudioFrame(pcm, channels.value, samplingRate.value)
  }

  def muteSelfAudio(): Unit = {
    selfState = selfState.copy(audioMuted = true)
    ToxSingleton.toxAv.setAudioBitRate(callNumber.value, BitRate.Disabled)
    audioCapture.stop()
  }

  def unmuteSelfAudio(): Unit = {
    selfState = selfState.copy(audioMuted = false)
    ToxSingleton.toxAv.setAudioBitRate(callNumber.value, selfState.audioBitRate)
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
    ToxSingleton.toxAv.callControl(callNumber.value, ToxavCallControl.MUTE_AUDIO)
  }

  def unmuteFriendAudio(): Unit = {
    ToxSingleton.toxAv.callControl(callNumber.value, ToxavCallControl.UNMUTE_AUDIO)
  }

  def hideFriendVideo(): Unit = {
    ToxSingleton.toxAv.callControl(callNumber.value, ToxavCallControl.HIDE_VIDEO)
  }

  def showFriendVideo(): Unit = {
    ToxSingleton.toxAv.callControl(callNumber.value, ToxavCallControl.SHOW_VIDEO)
  }

  def end(error: Boolean = false): Unit = {
    logCallEvent(s"ended error:$error")
    // only send a call control if the call wasn't ended unexpectedly
    if (!error) {
      ToxSingleton.toxAv.callControl(callNumber.value, ToxavCallControl.CANCEL)
    }

    updateFriendState(Set(ToxavFriendCallState.FINISHED))

    audioCapture.stop()
    cleanUp()
  }

  private def cleanUp(): Unit = {
    audioPlayer.cleanUp()
    audioCapture.cleanUp()
  }
}
