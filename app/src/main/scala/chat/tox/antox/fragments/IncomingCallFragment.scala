package chat.tox.antox.fragments

import android.content.Context
import android.media.{AudioManager, MediaPlayer, RingtoneManager}
import android.os.{Bundle, Vibrator}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.LinearLayout
import chat.tox.antox.R
import chat.tox.antox.activities.CallReplyDialog
import chat.tox.antox.av.Call
import chat.tox.antox.utils.AntoxLog
import chat.tox.antox.wrapper.ContactKey

object IncomingCallFragment {

  def newInstance(call: Call, activeKey: ContactKey): IncomingCallFragment = {
    val incomingCallFragment = new IncomingCallFragment()

    val bundle = new Bundle()
    bundle.putInt(CommonCallFragment.EXTRA_CALL_NUMBER, call.callNumber.value)
    bundle.putString(CommonCallFragment.EXTRA_ACTIVE_KEY, activeKey.toString)
    bundle.putInt(CommonCallFragment.EXTRA_FRAGMENT_LAYOUT, R.layout.fragment_incoming_call)
    incomingCallFragment.setArguments(bundle)

    incomingCallFragment
  }
}

class IncomingCallFragment extends CommonCallFragment {

  var vibrator: Vibrator = _
  val vibrationPattern = Array[Long](0, 1000, 1000) //wait 0ms vibrate 1000ms off 1000ms

  var audioManager: AudioManager = _

  var answerCallButton: View = _

  var maybeRingtone: Option[MediaPlayer] = None

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    audioManager = getActivity.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]

    vibrator = getActivity.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]
    vibrator.vibrate(vibrationPattern, 0)

    val maybeRingtoneUri = Option(RingtoneManager.getActualDefaultRingtoneUri(getActivity, RingtoneManager.TYPE_RINGTONE))

    maybeRingtoneUri.foreach(ringtoneUri => {
      val ringtone =
        try {
          val tempRingtone = new MediaPlayer()
          tempRingtone.setDataSource(getActivity, ringtoneUri)
          tempRingtone.setAudioStreamType(AudioManager.STREAM_RING)
          tempRingtone.setLooping(true)
          tempRingtone.prepare()
          tempRingtone
        } catch {
          case e: Exception =>
            MediaPlayer.create(getActivity, R.raw.incoming_call)
        }
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

    val replyButton = rootView.findViewById(R.id.incoming_call_reply).asInstanceOf[LinearLayout]
    replyButton.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        val callReplyDialogTag = "call_reply_dialog"
        new CallReplyDialog().show(getFragmentManager, callReplyDialogTag)
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
    maybeRingtone.foreach(_.release())
  }
}
