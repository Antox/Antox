package chat.tox.antox.wrapper

import chat.tox.antox.tox.Intervals
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.av.data.{AudioChannels, BitRate, SampleCount, SamplingRate}
import im.tox.tox4j.av.enums.ToxavCallControl
import im.tox.tox4j.av.exceptions.ToxavCallControlException
import im.tox.tox4j.core.data.ToxFriendNumber
import im.tox.tox4j.impl.jni.{ToxAvImpl, ToxCoreImpl}

class ToxAv(core: ToxCoreImpl) extends Intervals {

  val toxAv = new ToxAvImpl(core)

  def close(): Unit = toxAv.close()

  def iterate(avEventListener: ToxAvEventListener[Unit]): Unit = toxAv.iterate(avEventListener)(Unit)

  override def interval: Int = toxAv.iterationInterval

  @throws[ToxavCallControlException]
  def callControl(callNumber: CallNumber, control: ToxavCallControl): Unit = toxAv.callControl(ToxFriendNumber.unsafeFromInt(callNumber.value), control)

  def answer(callNumber: CallNumber, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    toxAv.answer(ToxFriendNumber.unsafeFromInt(callNumber.value), audioBitRate, videoBitRate)

  def call(callNumber: CallNumber, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    toxAv.call(ToxFriendNumber.unsafeFromInt(callNumber.value), audioBitRate, videoBitRate)

  def audioSendFrame(callNumber: CallNumber, pcm: Array[Short], sampleCount: SampleCount,
                     channels: AudioChannels, samplingRate: SamplingRate): Unit =
    toxAv.audioSendFrame(ToxFriendNumber.unsafeFromInt(callNumber.value), pcm, sampleCount, channels, samplingRate)

  def videoSendFrame(callNumber: CallNumber, width: Int, height: Int, y: Array[Byte],
                     u: Array[Byte], v: Array[Byte]): Unit =
    toxAv.videoSendFrame(ToxFriendNumber.unsafeFromInt(callNumber.value), width, height, y, u, v)

  def setAudioBitRate(callNumber: CallNumber, bitRate: BitRate): Unit =
    toxAv.setBitRate(ToxFriendNumber.unsafeFromInt(callNumber.value), bitRate, BitRate.Unchanged)

  def setVideoBitRate(callNumber: CallNumber, bitRate: BitRate): Unit =
    toxAv.setBitRate(ToxFriendNumber.unsafeFromInt(callNumber.value), BitRate.Unchanged, bitRate)
}