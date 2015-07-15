package im.tox.antox.wrapper

import im.tox.antox.tox.{ToxSingleton, Intervals}
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.av.enums.ToxCallControl
import im.tox.tox4j.impl.jni.{ToxCoreImpl, ToxAvImpl}
import scala.collection.JavaConversions._

class ToxAv(core: ToxCoreImpl) extends Intervals {

  val toxAv: ToxAvImpl = new ToxAvImpl(core)

  def close(): Unit = toxAv.close()

  def iterate(): Unit = toxAv.iterate()

  override def interval: Int = toxAv.iterationInterval / 4

  def activeCall = ToxSingleton.getAntoxFriendList.all.find(p => p.call.active)

  def onHoldCall = ToxSingleton.getAntoxFriendList.all.find(p => p.call.onHold)

  def answer(friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Unit =
    toxAv.answer(friendNumber, audioBitRate, videoBitRate)

  def call(friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Unit =
    toxAv.call(friendNumber, audioBitRate, videoBitRate)

  def callback(handler: ToxAvEventListener): Unit = toxAv.callback(handler)

  def callbackCall(callback: CallCallback): Unit = toxAv.callbackCall(callback)

  def callbackReceiveAudioFrame(callback: AudioReceiveFrameCallback): Unit =
    toxAv.callbackAudioReceiveFrame(callback)

  def callbackReceiveVideoFrame(callback: VideoReceiveFrameCallback): Unit =
    toxAv.callbackVideoReceiveFrame(callback)

  def callControl(friendNumber: Int, control: ToxCallControl): Unit =
    toxAv.callControl(friendNumber, control)

  def audioSendFrame(friendNumber: Int, pcm: Array[Short], sampleCount: Int,
                     channels: Int, samplingRate: Int): Unit =
    toxAv.audioSendFrame(friendNumber, pcm, sampleCount, channels, samplingRate)

  def videoSendFrame(friendNumber: Int, width: Int, height: Int, y: Array[Byte],
                     u: Array[Byte], v: Array[Byte], a: Array[Byte]): Unit =
    toxAv.videoSendFrame(friendNumber, width, height, y, u, v, a)

  def audioBitRateSet(friendNumber: Int, bitRate: Int, force: Boolean): Unit =
    toxAv.audioBitRateSet(friendNumber, bitRate, force)

  def videoBitRateSet(friendNumber: Int, bitRate: Int, force: Boolean): Unit =
    toxAv.videoBitRateSet(friendNumber, bitRate, force)

  def callbackCallState(callback: CallStateCallback): Unit =
    toxAv.callbackCallState(callback)

  def callbackVideoBitRateStatus(callback: VideoBitRateStatusCallback): Unit =
    toxAv.callbackVideoBitRateStatus(callback)

  def callbackAudioBitRateStatus(callback: AudioBitRateStatusCallback): Unit =
    toxAv.callbackAudioBitRateStatus(callback)
}