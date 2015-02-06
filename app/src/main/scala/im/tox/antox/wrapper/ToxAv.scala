package im.tox.antox.wrapper

import im.tox.antox.data.State
import im.tox.antox.utils.{AudioCapture, Call, CallList, AntoxFriendList}
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.av.enums.ToxCallControl
import im.tox.tox4j.{ToxAvImpl, ToxCoreImpl}
import rx.lang.scala.schedulers.IOScheduler

class ToxAv(core: ToxCoreImpl) {

  val toxAv: ToxAvImpl = new ToxAvImpl(core)

  val calls: CallList = new CallList()

  def getCallList: CallList = calls

  def close(): Unit = toxAv.close()

  override def finalize(): Unit = toxAv.finalize()

  def iterationInterval(): Int = toxAv.iterationInterval()

  def iteration(): Unit = toxAv.iteration()

  def call(friendNumber: Int, audioBitrate: Int, videoBitrate: Int): Unit = toxAv.call(friendNumber, audioBitrate, videoBitrate)

  def callbackCall(callback: CallCallback): Unit = toxAv.callbackCall(callback)

  def answer(friendNumber: Int, audioBitrate: Int, videoBitrate: Int): Unit = {
    toxAv.answer(friendNumber, audioBitrate, videoBitrate)
    val call = new Call(friendNumber, audioBitrate, videoBitrate)
    call.start()
    calls.add(call)
  }

  def callControl(friendNumber: Int, control: ToxCallControl): Unit = toxAv.callControl(friendNumber, control)

  def callbackCallControl(callback: CallStateCallback): Unit = toxAv.callbackCallControl(callback)

  def setAudioBitRate(friendNumber: Int, audioBitrate: Int): Unit = toxAv.setAudioBitRate(friendNumber, audioBitrate)

  def setVideoBitRate(friendNumber: Int, videoBitrate: Int): Unit = toxAv.setVideoBitRate(friendNumber, videoBitrate)

  def callbackRequestVideoFrame(callback: RequestVideoFrameCallback): Unit = toxAv.callbackRequestVideoFrame(callback)

  def sendVideoFrame(friendNumber: Int, width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte], a: Array[Byte]): Unit = toxAv.sendVideoFrame(friendNumber, width, height, y, u, v, a)

  def callbackRequestAudioFrame(callback: RequestAudioFrameCallback): Unit = toxAv.callbackRequestAudioFrame(callback)

  def sendAudioFrame(friendNumber: Int, pcm: Array[Short], sampleCount: Int, channels: Int, samplingRate: Int): Unit = toxAv.sendAudioFrame(friendNumber, pcm, sampleCount, channels, samplingRate)

  def callbackReceiveVideoFrame(callback: ReceiveVideoFrameCallback): Unit = toxAv.callbackReceiveVideoFrame(callback)

  def callbackReceiveAudioFrame(callback: ReceiveAudioFrameCallback): Unit = toxAv.callbackReceiveAudioFrame(callback)

  def callback(handler: ToxAvEventListener): Unit = toxAv.callback(handler)
}