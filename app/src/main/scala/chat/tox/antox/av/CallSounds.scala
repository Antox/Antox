package chat.tox.antox.av

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer.OnCompletionListener
import android.media.{AudioManager, MediaPlayer, RingtoneManager}
import chat.tox.antox.R
import chat.tox.antox.utils.MediaUtils

/**
  * Attach to a call and add sounds for the appropriate call events.
  */
class CallSounds(val call: Call, val context: Context) extends CallEnhancement {

  val ended = MediaUtils.setupSound(context, R.raw.end_call, AudioManager.STREAM_VOICE_CALL, looping = false)
  val ringback: MediaPlayer = MediaUtils.setupSound(context, R.raw.ringback_tone, AudioManager.STREAM_VOICE_CALL, looping = true)
  val maybeRingtone = findRingtone()

  def findRingtone(): Option[MediaPlayer] = {
    val maybeRingtoneUri = Option(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE))

    maybeRingtoneUri.map(ringtoneUri => {
      val ringtone =
        try {
          val tempRingtone = new MediaPlayer()
          tempRingtone.setDataSource(context, ringtoneUri)
          tempRingtone
        } catch {
          case e: Exception =>
            val afd: AssetFileDescriptor = context.getResources.openRawResourceFd(R.raw.incoming_call)
            val backupRingtone: MediaPlayer = new MediaPlayer()
            backupRingtone.setDataSource(afd.getFileDescriptor, afd.getStartOffset, afd.getLength)
            backupRingtone
        }

      ringtone.setAudioStreamType(AudioManager.STREAM_RING)
      ringtone.setLooping(true)
      ringtone.prepare()
      ringtone
    })
  }

  // Add subscriptions for call events mapping to sounds

  subscriptions +=
    call.ringingObservable.distinctUntilChanged.subscribe(ringing => {
      if (call.incoming) {
        if (ringing) {
          maybeRingtone.foreach(_.start())
        } else {
          maybeRingtone.foreach(_.stop())
        }
      } else {
        if (ringing) {
          ringback.start()
        } else {
          ringback.stop()
        }
      }
    })

  subscriptions +=
    call.endedObservable.subscribe(_ => {
      onEnd()
    })

  private def onEnd(): Unit = {
    ended.start()
    ended.setOnCompletionListener(new OnCompletionListener {
      override def onCompletion(mp: MediaPlayer): Unit = {
        mp.release()
      }
    })

    release()
  }

  private def release(): Unit = {
    subscriptions.unsubscribe()

    ringback.release()
    maybeRingtone.foreach(_.release())
  }

}
