package im.tox.tox4j.impl.jni

import java.util

import com.typesafe.scalalogging.Logger
import im.tox.tox4j.av._
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.av.data._
import im.tox.tox4j.av.enums.{ ToxavCallControl, ToxavFriendCallState }
import im.tox.tox4j.av.exceptions._
import im.tox.tox4j.core.ToxCore
import im.tox.tox4j.core.data.ToxFriendNumber
import im.tox.tox4j.impl.jni.ToxAvImpl.logger
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory

private object ToxAvImpl {
  private val logger = Logger(LoggerFactory.getLogger(getClass))
}

/**
 * Initialise an A/V session for the existing Tox instance.
 *
 * @param tox An instance of the C-backed ToxCore implementation.
 */
// scalastyle:off no.finalize
@throws[ToxavNewException]("If there was already an A/V session.")
final class ToxAvImpl(@NotNull private val tox: ToxCoreImpl) extends ToxAv {

  private[this] val onClose = tox.addOnCloseCallback(close)

  private[jni] val instanceNumber = ToxAvJni.toxavNew(tox.instanceNumber)

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  override def create(tox: ToxCore): ToxAv = {
    try {
      new ToxAvImpl(tox.asInstanceOf[ToxCoreImpl])
    } catch {
      case _: ClassCastException =>
        throw new ToxavNewException(ToxavNewException.Code.INCOMPATIBLE, tox.getClass.getCanonicalName)
    }
  }

  override def close(): Unit = {
    tox.removeOnCloseCallback(onClose)
    ToxAvJni.toxavKill(instanceNumber)
  }

  protected override def finalize(): Unit = {
    try {
      ToxAvJni.toxavFinalize(instanceNumber)
    } catch {
      case e: Throwable =>
        logger.error("Exception caught in finalizer; this indicates a serious problem in native code", e)
    }
    super.finalize()
  }

  override def iterate[S](@NotNull handler: ToxAvEventListener[S])(state: S): S = {
    ToxAvEventDispatch.dispatch(handler, ToxAvJni.toxavIterate(instanceNumber))(state)
  }

  override def iterationInterval: Int =
    ToxAvJni.toxavIterationInterval(instanceNumber)

  @throws[ToxavCallException]
  override def call(friendNumber: ToxFriendNumber, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    ToxAvJni.toxavCall(instanceNumber, friendNumber.value, audioBitRate.value, videoBitRate.value)

  @throws[ToxavAnswerException]
  override def answer(friendNumber: ToxFriendNumber, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    ToxAvJni.toxavAnswer(instanceNumber, friendNumber.value, audioBitRate.value, videoBitRate.value)

  @throws[ToxavCallControlException]
  override def callControl(friendNumber: ToxFriendNumber, control: ToxavCallControl): Unit =
    ToxAvJni.toxavCallControl(instanceNumber, friendNumber.value, control.ordinal)

  @throws[ToxavBitRateSetException]
  override def setBitRate(friendNumber: ToxFriendNumber, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    ToxAvJni.toxavBitRateSet(instanceNumber, friendNumber.value, audioBitRate.value, videoBitRate.value)

  @throws[ToxavSendFrameException]
  override def audioSendFrame(
    friendNumber: ToxFriendNumber,
    pcm: Array[Short],
    sampleCount: SampleCount,
    channels: AudioChannels,
    samplingRate: SamplingRate
  ): Unit = {
    ToxAvJni.toxavAudioSendFrame(instanceNumber, friendNumber.value, pcm, sampleCount.value, channels.value, samplingRate.value)
  }

  @throws[ToxavSendFrameException]
  override def videoSendFrame(
    friendNumber: ToxFriendNumber,
    width: Int, height: Int,
    y: Array[Byte], u: Array[Byte], v: Array[Byte]
  ): Unit = {
    ToxAvJni.toxavVideoSendFrame(instanceNumber, friendNumber.value, width, height, y, u, v)
  }

  def invokeAudioReceiveFrame(friendNumber: ToxFriendNumber, pcm: Array[Short], channels: AudioChannels, samplingRate: SamplingRate): Unit =
    ToxAvJni.invokeAudioReceiveFrame(instanceNumber, friendNumber.value, pcm, channels.value, samplingRate.value)
  def invokeBitRateStatus(friendNumber: ToxFriendNumber, audioBitRate: BitRate, videoBitRate: BitRate): Unit =
    ToxAvJni.invokeBitRateStatus(instanceNumber, friendNumber.value, audioBitRate.value, videoBitRate.value)
  def invokeCall(friendNumber: ToxFriendNumber, audioEnabled: Boolean, videoEnabled: Boolean): Unit =
    ToxAvJni.invokeCall(instanceNumber, friendNumber.value, audioEnabled, videoEnabled)
  def invokeCallState(friendNumber: ToxFriendNumber, callState: util.EnumSet[ToxavFriendCallState]): Unit =
    ToxAvJni.invokeCallState(instanceNumber, friendNumber.value, ToxAvEventDispatch.convert(callState))
  def invokeVideoReceiveFrame(friendNumber: ToxFriendNumber, width: Width, height: Height, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int): Unit = // scalastyle:ignore line.size.limit
    ToxAvJni.invokeVideoReceiveFrame(instanceNumber, friendNumber.value, width.value, height.value, y, u, v, yStride, uStride, vStride)

}
