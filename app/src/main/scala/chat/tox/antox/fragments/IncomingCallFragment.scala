package chat.tox.antox.fragments

import android.content.Context
import android.media.AudioManager
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

  var answerCallButton: View = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    vibrator = getActivity.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]
    vibrator.vibrate(vibrationPattern, 0)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)

    rootView.findViewById(R.id.call_duration).setVisibility(View.GONE)

    /* Set up the answer and av buttons */
    answerCallButton = rootView.findViewById(R.id.answer_call_circle)

    answerCallButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.answerCall()
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
    AntoxLog.debug("Audio stream volume " + audioManager.getStreamVolume(AudioManager.STREAM_RING))

    callStateView.setVisibility(View.VISIBLE)

    if (call.selfState.receivingVideo) {
      callStateView.setText(R.string.call_incoming_video)
    } else {
      callStateView.setText(R.string.call_incoming_voice)
    }

    rootView
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    vibrator.cancel()
  }
}
