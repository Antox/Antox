package chat.tox.antox.fragments

import android.content.Context
import android.media.{RingtoneManager, AudioManager, MediaPlayer}
import android.os.{Bundle, Vibrator}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import chat.tox.antox.R
import chat.tox.antox.av.Call
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.ContactKey

class IncomingCallFragment(call: Call, activeKey: ContactKey) extends CommonCallFragment(call, activeKey, R.layout.fragment_incoming_call) {

  var vibrator: Vibrator = _
  val vibrationPattern = Array[Long](0, 1000, 1000) //wait 0ms vibrate 1000ms off 1000ms

  var audioManager: AudioManager = _

  var answerCallButton: View = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    audioManager = getActivity.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]

    vibrator = getActivity.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]
    vibrator.vibrate(vibrationPattern, 0)

    val maybeRingtoneUri = Option(RingtoneManager.getActualDefaultRingtoneUri(getActivity, RingtoneManager.TYPE_RINGTONE))

    maybeRingtoneUri.foreach(ringtoneUri => {
      val ringtone = new MediaPlayer()
      ringtone.setDataSource(getActivity, ringtoneUri)
      ringtone.setAudioStreamType(AudioManager.STREAM_RING)
      ringtone.setLooping(true)
      ringtone.prepare()
      maybeRingtone = Some(ringtone)
    })
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)

    rootView.findViewById(R.id.call_duration).setVisibility(View.GONE)

    /* Set up the answer and av buttons */
    answerCallButton = rootView.findViewById(R.id.answer_call_circle)

    answerCallButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.answerCall(receivingAudio = true, receivingVideo = false)
      }
    })

    // vibrate and ring on incoming call
    maybeRingtone.foreach(_.start())
    AntoxLog.debug("Audio stream volume " + audioManager.getStreamVolume(AudioManager.STREAM_RING))

    callStateView.setVisibility(View.VISIBLE)
    callStateView.setText(R.string.call_incoming)

    rootView
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    vibrator.cancel()
    maybeRingtone.foreach(_.stop())
  }
}
