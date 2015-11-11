package chat.tox.antox.wrapper

import chat.tox.antox.tox.Intervals
import im.tox.tox4j.av.{SamplingRate, AudioChannels, SampleCount, BitRate}
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.av.enums.ToxavCallControl
import im.tox.tox4j.av.exceptions.ToxavCallControlException
import im.tox.tox4j.impl.jni.{ToxAvImpl, ToxCoreImpl}

class ToxAv(core: ToxCoreImpl[Unit]) extends Intervals {

  val toxAv = new ToxAvImpl[Unit](core)

  def close(): Unit = toxAv.close()

  def iterate(): Unit = toxAv.iterate(Unit)

  override def interval: Int = toxAv.iterationInterval / 4

  @throws[ToxavCallControlException]
  def callControl(friendNumber: Int, control: ToxavCallControl): Unit = toxAv.callControl(friendNumber, control)

  def answer(friendNumber: Int, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    toxAv.answer(friendNumber, audioBitRate, videoBitRate)

  def call(friendNumber: Int, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    toxAv.call(friendNumber, audioBitRate, videoBitRate)

  def callback(handler: ToxAvEventListener[Unit]): Unit = toxAv.callback(handler)

  def audioSendFrame(friendNumber: Int, pcm: Array[Short], sampleCount: SampleCount,
                     channels: AudioChannels, samplingRate: SamplingRate): Unit =
    toxAv.audioSendFrame(friendNumber, pcm, sampleCount, channels, samplingRate)

  def videoSendFrame(friendNumber: Int, width: Int, height: Int, y: Array[Byte],
                     u: Array[Byte], v: Array[Byte]): Unit =
    toxAv.videoSendFrame(friendNumber, width, height, y, u, v)

  def setAudioBitRate(friendNumber: Int, bitRate: BitRate): Unit =
    toxAv.setBitRate(friendNumber, bitRate, BitRate.Unchanged)

  def setVideoBitRate(friendNumber: Int, bitRate: BitRate): Unit =
    toxAv.setBitRate(friendNumber, BitRate.Unchanged, bitRate)
}