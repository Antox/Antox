package chat.tox.antox.av

import android.content.Context
import android.media.MediaPlayer.OnCompletionListener
import android.media.{AudioManager, MediaPlayer, RingtoneManager}
import chat.tox.antox.R
import chat.tox.antox.utils.MediaUtils
import rx.lang.scala.subscriptions.CompositeSubscription

/**
  * Attach to a call and add sounds for the appropriate call events.
  */
class CallSounds(call: Call, context: Context) extends CallEnhancement {

  val ended = MediaUtils.setupSound(context, R.raw.end_call, AudioManager.STREAM_VOICE_CALL, looping = false)
  val ringback: MediaPlayer = MediaUtils.setupSound(context, R.raw.ringback_tone, AudioManager.STREAM_VOICE_CALL, looping = true)
  val maybeRingtone = findRingtone()

  def findRingtone(): Option[MediaPlayer] = {
    val maybeRingtoneUri = Option(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE))

    maybeRingtoneUri.map(ringtoneUri => {
      try {
        val tempRingtone = new MediaPlayer()
        tempRingtone.setDataSource(context, ringtoneUri)
        tempRingtone.setAudioStreamType(AudioManager.STREAM_RING)
        tempRingtone.setLooping(true)
        tempRingtone.prepare()
        tempRingtone
      } catch {
        case e: Exception =>
          MediaPlayer.create(context, R.raw.incoming_call)
      }
    })
  }

  // Add subscriptions for call events mapping to sounds
  val subscriptions: CompositeSubscription = CompositeSubscription()

  subscriptions +=
    call.callEndedObservable.subscribe(_ => {
      ended.start()
    })

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

  private def release(): Unit = {
    subscriptions.unsubscribe()

    ringback.release()
    maybeRingtone.foreach(_.release())
  }

  /**
    * Called when the call ends.
    */
  override def onRemove(): Unit = {
    ended.start()
    ended.setOnCompletionListener(new OnCompletionListener {
      override def onCompletion(mp: MediaPlayer): Unit = {
        mp.release()
      }
    })

    release()
  }
}
