package chat.tox.antox.av

import java.util.concurrent.TimeUnit

import chat.tox.antox.av.CallEndReason.CallEndReason
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{AntoxLog, AudioCapture}
import chat.tox.antox.wrapper.{CallNumber, ContactKey}
import im.tox.tox4j.av._
import im.tox.tox4j.av.enums.{ToxavCallControl, ToxavFriendCallState}
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.JavaConversions._
import rx.lang.scala.schedulers.NewThreadScheduler
import rx.lang.scala.subjects.BehaviorSubject
import rx.lang.scala.{Observable, Subject}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.util.Try

final case class Call(callNumber: CallNumber, contactKey: ContactKey, incoming: Boolean) {

  private val friendStateSubject = BehaviorSubject[Set[ToxavFriendCallState]](Set.empty[ToxavFriendCallState])
  private def friendState: Set[ToxavFriendCallState] = friendStateSubject.getValue

  // only describes self state
  private val selfStateSubject = BehaviorSubject[SelfCallState](SelfCallState.DEFAULT)
  def selfStateObservable: Observable[SelfCallState] = selfStateSubject.asJavaObservable
  private def selfState = selfStateSubject.getValue

  //monitors both friend and self state, but does not expose them
  val callStateObservable = friendStateSubject.merge(selfStateSubject).map(_ => Unit)

  // is video enabled in any way
  val callVideoObservable = selfStateObservable.map(state => state.sendingVideo || state.receivingVideo)

  private val callEndedSubject = Subject[CallEndReason]()
  // called only once, when the call ends with the reason it ended
  def callEndedObservable: Observable[CallEndReason] = callEndedSubject.asJavaObservable

  //only for outgoing audio
  private val samplingRate = SamplingRate.Rate48k //in Hz
  private val audioLength = AudioLength.Length20 //in milliseconds
  private val channels = AudioChannels.Stereo

  val audioBufferLength = 3 // in frames
  val videoBufferLength = 3 // in frames

  val defaultRingTime = Duration(30, TimeUnit.SECONDS)

  // ringing by default (call should only be created if it is ringing)
  private val ringingSubject = BehaviorSubject[Boolean](true)
  def ringingObservable: Observable[Boolean] = ringingSubject.asJavaObservable
  def ringing = ringingSubject.getValue

  var startTime: Duration = Duration(0, TimeUnit.MILLISECONDS)
  def duration: Duration = Duration(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - startTime //in milliseconds

  val callEnhancements: ArrayBuffer[CallEnhancement] = new ArrayBuffer()

  /**
   * Describes a state in which the call is not FINISHED or ERROR.
   * When the call is on hold or ringing (not yet answered) this will return true.
   */
  def active: Boolean = isActive(friendState) && !selfState.ended

  private def isActive(state: Set[ToxavFriendCallState]): Boolean = {
    !state.contains(ToxavFriendCallState.FINISHED) && !state.contains(ToxavFriendCallState.ERROR)
  }

  def onHold: Boolean = friendState.isEmpty

  val audioCapture: AudioCapture = new AudioCapture(samplingRate.value, channels.value)
  val audioPlayer = new AudioPlayer(samplingRate.value, channels.value, audioBufferLength)

  private val videoFrameSubject = Subject[YuvVideoFrame]()
  def videoFrameObservable: Observable[YuvVideoFrame] = videoFrameSubject.asJavaObservable

  private def frameSize = SampleCount(audioLength, samplingRate)

  private def logCallEvent(event: String): Unit = AntoxLog.debug(s"Call $callNumber belonging to $contactKey $event")

  // make sure the call ends eventually if it's still ringing
  endAfterTime(defaultRingTime)

  def startCall(sendingAudio: Boolean, sendingVideo: Boolean): Unit = {
    logCallEvent(s"started sending audio:$sendingAudio and video:$sendingVideo")
    ToxSingleton.toxAv.call(
      callNumber.value,
      if (sendingAudio) selfState.audioBitRate else BitRate.Disabled,
      if (sendingVideo) selfState.videoBitRate else BitRate.Disabled)

    selfStateSubject.onNext(selfState.copy(audioMuted = !sendingAudio, videoHidden = !sendingVideo))
  }

  def answerCall(sendingAudio: Boolean, sendingVideo: Boolean): Unit = {
    logCallEvent(s"answered sending audio:$sendingAudio and video:$sendingVideo")

    ToxSingleton.toxAv.answer(callNumber.value, selfState.audioBitRate, selfState.videoBitRate)
    selfStateSubject.onNext(selfState.copy(audioMuted = !sendingAudio, videoHidden = !sendingVideo))

    callStarted()
    ringingSubject.onNext(false)
  }

  def onIncoming(audioEnabled: Boolean, videoEnabled: Boolean): Unit = {
    logCallEvent(s"incoming receiving audio:$audioEnabled and video:$videoEnabled")

    selfStateSubject.onNext(selfState.copy(receivingAudio = audioEnabled, receivingVideo = videoEnabled))
  }

  private def endAfterTime(ringTime: Duration): Unit = {
    //end the call after `ringTime`
    Observable
      .timer(defaultRingTime)
      .subscribeOn(NewThreadScheduler())
      .foreach(_ => {
        if (active && ringing) {
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
      })
  }

  def updateFriendState(state: Set[ToxavFriendCallState]): Unit = {
    logCallEvent(s"friend call state updated to $state")

    val answered: Boolean = friendState.isEmpty && isActive(state) && !incoming
    val ended: Boolean = !isActive(state)

    val newSelfState =
      selfState.copy(
        receivingAudio = state.contains(ToxavFriendCallState.SENDING_A),
        receivingVideo = state.contains(ToxavFriendCallState.SENDING_V)
      )

    if (answered) {
      callStarted()
      ringingSubject.onNext(false)
    } else if (ended) {
      end()
    } else {
      if (newSelfState != selfState) selfStateSubject.onNext(newSelfState)
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
                //if (active) end(reason = CallEndReason.Error)
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

  def onVideoFrame(videoFrame: YuvVideoFrame): Unit = {
    videoFrameSubject.onNext(videoFrame)
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
    logCallEvent(s"ended reason:$reason")

    // only send a call control if the call wasn't ended unexpectedly
    if (reason != CallEndReason.Error) {
      Try(ToxSingleton.toxAv.callControl(callNumber.value, ToxavCallControl.CANCEL))
    }

    selfStateSubject.onNext(selfState.copy(ended = true))

    callEndedSubject.onNext(reason)
    callEndedSubject.onCompleted()

    onCallEnded()
  }

  private def onCallEnded(): Unit = {
    audioCapture.stop()
    cleanUp()
  }

  private def cleanUp(): Unit = {
    audioPlayer.cleanUp()
    audioCapture.cleanUp()
  }
}
