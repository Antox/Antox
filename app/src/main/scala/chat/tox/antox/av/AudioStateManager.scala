package chat.tox.antox.av

import android.media.AudioManager

class AudioStateManager(val audioManager: AudioManager) extends CallEnhancement {
  private val initialMode: Int = audioManager.getMode
  private val initialSpeakerphone: Boolean = audioManager.isSpeakerphoneOn

  private def restoreAudioState(): Unit = {
    audioManager.setMode(initialMode)
    audioManager.setSpeakerphoneOn(initialSpeakerphone)
  }

  /**
   * Called when the call ends.
   */
  override def onRemove(): Unit = {
    restoreAudioState()
  }
}
