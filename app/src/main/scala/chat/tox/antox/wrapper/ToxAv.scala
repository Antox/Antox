package chat.tox.antox.wrapper

import chat.tox.antox.av.Call
import chat.tox.antox.tox.{ToxSingleton, Intervals}
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.impl.jni.{ToxAvImpl, ToxCoreImpl}

import scala.collection.JavaConversions._

class ToxAv(core: ToxCoreImpl[Unit]) extends Intervals {

  val toxAv = new ToxAvImpl[Unit](core)

  def close(): Unit = toxAv.close()

  def iterate(): Unit = toxAv.iterate()

  override def interval: Int = toxAv.iterationInterval / 4

  def activeCall: Option[Call] = ToxSingleton.getAntoxFriendList.all.find(p => p.call.active).map(_.call)

  def onHoldCall: Option[Call] = ToxSingleton.getAntoxFriendList.all.find(p => p.call.onHold).map(_.call)

  def answer(friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Unit =
    toxAv.answer(friendNumber, audioBitRate, videoBitRate)

  def call(friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Unit =
    toxAv.call(friendNumber, audioBitRate, videoBitRate)

  def callback(handler: ToxAvEventListener[Unit]): Unit = toxAv.callback(handler)

  def audioSendFrame(friendNumber: Int, pcm: Array[Short], sampleCount: Int,
                     channels: Int, samplingRate: Int): Unit =
    toxAv.audioSendFrame(friendNumber, pcm, sampleCount, channels, samplingRate)

  def videoSendFrame(friendNumber: Int, width: Int, height: Int, y: Array[Byte],
                     u: Array[Byte], v: Array[Byte]): Unit =
    toxAv.videoSendFrame(friendNumber, width, height, y, u, v)

  def audioBitRateSet(friendNumber: Int, bitRate: Int, force: Boolean): Unit =
    toxAv.setAudioBitRate(friendNumber, bitRate, force)

  def videoBitRateSet(friendNumber: Int, bitRate: Int, force: Boolean): Unit =
    toxAv.setVideoBitRate(friendNumber, bitRate, force)
}