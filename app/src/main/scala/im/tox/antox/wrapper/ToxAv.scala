package im.tox.antox.wrapper

import im.tox.antox.data.State
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.av.enums.ToxCallControl
import im.tox.tox4j.{ToxAvImpl, ToxCoreImpl}
import rx.lang.scala.schedulers.IOScheduler

class ToxAv(core: ToxCoreImpl) {

  val toxAv: ToxAvImpl = new ToxAvImpl(core)

  def answer(friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Unit = toxAv.answer(friendNumber, audioBitRate, videoBitRate)

  def call(friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Unit = toxAv.call(friendNumber, audioBitRate, videoBitRate)

  def callback(handler: ToxAvEventListener): Unit = toxAv.callback(handler)

  def callbackCall(callback: CallCallback): Unit = toxAv.callbackCall(callback)

  def callbackCallControl(callback: CallStateCallback): Unit = toxAv.callbackCallControl(callback)

  def callbackReceiveAudioFrame(callback: ReceiveAudioFrameCallback): Unit = toxAv.callbackReceiveAudioFrame(callback)

  def callbackReceiveVideoFrame(callback: ReceiveVideoFrameCallback): Unit = toxAv.callbackReceiveVideoFrame(callback)

  def callControl(friendNumber: Int, control: ToxCallControl): Unit = toxAv.callControl(friendNumber, control)

  def close(): Unit = toxAv.close()

  override def finalize(): Unit = toxAv.finalize()

  def iteration(): Unit = toxAv.iteration()

  def iterationInterval(): Int = toxAv.iterationInterval()

  def sendAudioFrame(friendNumber: Int, pcm: Array[Short], sampleCount: Int, channels: Int, samplingRate: Int): Unit = toxAv.sendAudioFrame(friendNumber, pcm, sampleCount, channels, samplingRate)

  def sendVideoFrame(friendNumber: Int, width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte]): Unit = toxAv.sendVideoFrame(friendNumber, width, height, y, u, v)

  def setAudioBitRate(friendNumber: Int, bitRate: Int, force: Boolean): Unit = toxAv.setAudioBitRate(friendNumber, force, bitRate)

  def setVideoBitRate(friendNumber: Int, bitRate: Int, force: Boolean): Unit = toxAv.setVideoBitRate(friendNumber, force, bitRate)

}