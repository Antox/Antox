package im.tox.tox4j.impl.jni;

import im.tox.tox4j.av.exceptions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"checkstyle:emptylineseparator", "checkstyle:linelength"})
public final class ToxAvJni {

  static {
    ToxLoadJniLibrary.load("tox4j-c");
  }

  static native int toxavNew(int toxInstanceNumber) throws ToxavNewException;
  static native void toxavKill(int instanceNumber);
  static native void toxavFinalize(int instanceNumber);
  static native int toxavIterationInterval(int instanceNumber);
  @Nullable
  static native byte[] toxavIterate(int instanceNumber);
  static native void toxavCall(int instanceNumber, int friendNumber, int audioBitRate, int videoBitRate) throws ToxavCallException;
  static native void toxavAnswer(int instanceNumber, int friendNumber, int audioBitRate, int videoBitRate) throws ToxavAnswerException;
  static native void toxavCallControl(int instanceNumber, int friendNumber, int control) throws ToxavCallControlException;
  static native void toxavBitRateSet(int instanceNumber, int friendNumber, int audioBitRate, int videoBitRate) throws ToxavBitRateSetException;

  static native void toxavAudioSendFrame(
      int instanceNumber,
      int friendNumber,
      @NotNull short[] pcm, int sampleCount, int channels, int samplingRate
  ) throws ToxavSendFrameException;

  @SuppressWarnings("checkstyle:parametername")
  static native void toxavVideoSendFrame(
      int instanceNumber,
      int friendNumber,
      int width, int height,
      @NotNull byte[] y, @NotNull byte[] u, @NotNull byte[] v
  ) throws ToxavSendFrameException;

  static native void invokeAudioReceiveFrame(int instanceNumber, int friendNumber, short[] pcm, int channels, int samplingRate);
  static native void invokeBitRateStatus(int instanceNumber, int friendNumber, int audioBitRate, int videoBitRate);
  static native void invokeCall(int instanceNumber, int friendNumber, boolean audioEnabled, boolean videoEnabled);
  static native void invokeCallState(int instanceNumber, int friendNumber, int callState);
  @SuppressWarnings("checkstyle:parametername")
  static native void invokeVideoReceiveFrame(
      int instanceNumber,
      int friendNumber,
      int width, int height,
      @NotNull byte[] y, @NotNull byte[] u, @NotNull byte[] v,
      int yStride, int uStride, int vStride
  );

}
