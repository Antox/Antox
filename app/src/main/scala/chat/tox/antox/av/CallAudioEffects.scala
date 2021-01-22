package chat.tox.antox.av

import android.media.AudioRecord
import android.media.audiofx.{AcousticEchoCanceler, AutomaticGainControl, LoudnessEnhancer}

class CallAudioEffects(audioRecord: AudioRecord) {

  var automaticGainControl: Option[AutomaticGainControl] = Option(AutomaticGainControl.create(audioRecord.getAudioSessionId))
  var acousticEchoCanceller: Option[AcousticEchoCanceler] = Option(AcousticEchoCanceler.create(audioRecord.getAudioSessionId))
  var loudnessEnhancer: Option[LoudnessEnhancer] = None //disabled for the moment Option(new LoudnessEnhancer(audioRecord.getAudioSessionId))

  def enable(): Unit = {
    automaticGainControl.foreach(_.setEnabled(true))
    acousticEchoCanceller.foreach(_.setEnabled(true))
    loudnessEnhancer.foreach(le => {
      le.setEnabled(true)
      le.setTargetGain(2000)
    })
  }

  def cleanUp(): Unit = {
    loudnessEnhancer.foreach(_.release())
    automaticGainControl.foreach(_.release())
    acousticEchoCanceller.foreach(_.release())
  }
}
