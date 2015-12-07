package chat.tox.antox.av

import java.util.concurrent.TimeUnit

import chat.tox.antox.av.CallEndReason.CallEndReason
import chat.tox.antox.data.{CallEventKind, State}
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{AntoxLog, AudioCapture}
import chat.tox.antox.wrapper.{FriendKey, CallNumber, ContactKey}
import im.tox.tox4j.av._
import im.tox.tox4j.av.enums.{ToxavCallControl, ToxavFriendCallState}
import im.tox.tox4j.core.data.ToxNickname
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.{Observable, Subject}
import rx.lang.scala.subjects.BehaviorSubject

import rx.lang.scala.JavaConversions._

import scala.concurrent.duration.Duration

class Call(val callNumber: CallNumber, val contactKey: ContactKey) {

  private val friendStateSubject = BehaviorSubject[Set[ToxavFriendCallState]](Set.empty[ToxavFriendCallState])
  private def friendState: Set[ToxavFriendCallState] = friendStateSubject.getValue

  private val selfStateSubject = BehaviorSubject[SelfCallState](SelfCallState.DEFAULT)
  private def selfState = selfStateSubject.getValue

  //monitors both friend and self state, but does not expose them
  val callStateObservable = friendStateSubject.merge(selfStateSubject).map(_ => Unit)

  private val callEndedSubject = Subject[CallEndReason]()
  // called only once, when the call ends with the reason it ended
  def callEndedObservable: Observable[CallEndReason] = callEndedSubject.asJavaObservable

  //only for outgoing audio
  private val samplingRate = SamplingRate.Rate48k //in Hz
  private val audioLength = AudioLength.Length20 //in microseconds
  private val channels = AudioChannels.Stereo

  val defaultRingTime = Duration(5, TimeUnit.SECONDS)

  // ringing by default (call should only be created if it is ringing)
  private val ringingSubject = BehaviorSubject[Boolean](true)
  def ringingObservable: Observable[Boolean] = ringingSubject.asJavaObservable
  def ringing = ringingSubject.getValue
  var incoming = false

  var startTime: Duration = Duration(0, TimeUnit.MILLISECONDS)
  def duration: Duration = Duration(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - startTime //in milliseconds

  /**
   * Describes a state in which the call is not FINISHED or ERROR.
   * When the call is on hold or ringing (not yet answered) this will return true.
   */
  def active: Boolean = {
    isActive(friendState) && !selfState.ended
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
    ringingSubject.onNext(true)
  }

  def answerCall(receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    logCallEvent(s"answered receiving audio:$receivingAudio and video:$receivingVideo")

    ToxSingleton.toxAv.answer(callNumber.value, selfState.audioBitRate, selfState.videoBitRate)
    callStarted()
    ringingSubject.onNext(false)
  }

  def onIncoming(receivingAudio: Boolean, receivingVideo: Boolean): Unit = {
    logCallEvent(s"incoming receiving audio:$receivingAudio and video:$receivingVideo")

    endAfterTime(defaultRingTime)

    incoming = true
    ringingSubject.onNext(true)
    selfStateSubject.onNext(selfState.copy(receivingAudio = receivingAudio, receivingVideo = receivingVideo))
  }
  
  def endAfterTime(ringTime: Duration): Unit = {
    //end the call after `ringTime`
    new Thread(new Runnable {
      override def run(): Unit = {
        Thread.sleep(ringTime.toMillis)
        if (active && ringingSubject.getValue) {
          val reason =
            if (incoming) {
              // call was missed
              CallEndReason.Missed
            } else {
              // call was unanswered
              CallEndReason.Unanswered
            }

          end(reason)
        }
      }
    }).start()
  }

  def updateFriendState(state: Set[ToxavFriendCallState]): Unit = {
    logCallEvent(s"friend call state updated to $state")

    val answered: Boolean = friendState.isEmpty && isActive(state) && !incoming
    if (answered) {
      callStarted()
      ringingSubject.onNext(false)
    }

    friendStateSubject.onNext(friendState)
  }

  private def callStarted(): Unit = {
    startTime = Duration(System.currentTimeMillis(), TimeUnit.MILLISECONDS)

    logCallEvent(event = s"started at time $startTime")

    new Thread(new Runnable {
      override def run(): Unit = {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

        logCallEvent(s"audio send thread started")

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
                if (active) end(reason = CallEndReason.Error)
            }
          }

          val timeTaken = System.currentTimeMillis() - start
          if (timeTaken < audioLength.value.toMillis)
            Thread.sleep(audioLength.value.toMillis - timeTaken)
        }

        logCallEvent(s"audio send thread stopped")
      }
    }, "AudioSendThread").start()

    audioPlayer.start()
  }

  def onAudioFrame(pcm: Array[Short], channels: AudioChannels, samplingRate: SamplingRate): Unit = {
    audioPlayer.bufferAudioFrame(pcm, channels.value, samplingRate.value)
  }

  def muteSelfAudio(): Unit = {
    selfStateSubject.onNext(selfState.copy(audioMuted = true))
    ToxSingleton.toxAv.setAudioBitRate(callNumber.value, BitRate.Disabled)
    audioCapture.stop()
  }

  def unmuteSelfAudio(): Unit = {
    selfStateSubject.onNext(selfState.copy(audioMuted = false))
    ToxSingleton.toxAv.setAudioBitRate(callNumber.value, selfState.audioBitRate)
    audioCapture.start()
  }

  def hideSelfVideo(): Unit = {
    selfStateSubject.onNext(selfState.copy(videoHidden = true))
  }

  def showSelfVideo(): Unit = {
    selfStateSubject.onNext(selfState.copy(videoHidden = false))
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

  def end(reason: CallEndReason = CallEndReason.Normal): Unit = {
    assert(active)

    logCallEvent(s"ended reason:$reason")

    // only send a call control if the call wasn't ended unexpectedly
    if (reason != CallEndReason.Error) {
      ToxSingleton.toxAv.callControl(callNumber.value, ToxavCallControl.CANCEL)
    }

    selfStateSubject.onNext(selfState.copy(ended = true))

    callEndedSubject.onNext(reason)
    callEndedSubject.onCompleted()

    onCallEnded()
  }

  def onCallEnded(): Unit = {
    audioCapture.stop()
    cleanUp()
  }

  private def cleanUp(): Unit = {
    audioPlayer.cleanUp()
    audioCapture.cleanUp()
  }
}
