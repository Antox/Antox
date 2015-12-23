package chat.tox.antox.av

import android.media.AudioManager

class AudioStateManager(val call: Call, val audioManager: AudioManager) extends CallEnhancement {
  private val initialMode: Int = audioManager.getMode
  private val initialSpeakerphone: Boolean = false //audioManager.isSpeakerphoneOn always turn the speaker off for now

  private def restoreAudioState(): Unit = {
    audioManager.setMode(initialMode)
    audioManager.setSpeakerphoneOn(initialSpeakerphone)
  }

  subscriptions += call.ringingObservable.subscribe(ringing => {
    if (!ringing) {
      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION) // set the correct call mode on call start
    }
  })

  subscriptions += call.endedObservable.subscribe(_ => {
    restoreAudioState()
    subscriptions.unsubscribe()
  })
}
