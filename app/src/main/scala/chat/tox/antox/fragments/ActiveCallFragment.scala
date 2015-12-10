package chat.tox.antox.fragments

import android.content.Intent
import android.media.AudioManager
import android.os.{Bundle, SystemClock}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{Chronometer, ImageButton}
import chat.tox.antox.R
import chat.tox.antox.activities.ChatActivity
import chat.tox.antox.av.Call
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.ContactKey

object ActiveCallFragment {
  def newInstance(call: Call, activeKey: ContactKey): ActiveCallFragment = {
    val activeCallFragment = new ActiveCallFragment()

    val bundle = new Bundle()
    bundle.putInt(CommonCallFragment.EXTRA_CALL_NUMBER, call.callNumber.value)
    bundle.putString(CommonCallFragment.EXTRA_ACTIVE_KEY, activeKey.toString)
    bundle.putInt(CommonCallFragment.EXTRA_FRAGMENT_LAYOUT, R.layout.fragment_call)
    activeCallFragment.setArguments(bundle)

    activeCallFragment
  }
}

class ActiveCallFragment extends CommonCallFragment {

  var durationView: Chronometer = _
  var allToggleButtons: List[ImageButton] = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    getActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)

    /* Set up the speaker/mic buttons */
    val micOn = rootView.findViewById(R.id.mic_on).asInstanceOf[ImageButton]
    val micOff = rootView.findViewById(R.id.mic_off).asInstanceOf[ImageButton]
    val speakerOn = rootView.findViewById(R.id.speaker_on).asInstanceOf[ImageButton]
    val speakerOff = rootView.findViewById(R.id.speaker_off).asInstanceOf[ImageButton]
    val loudspeakerOn = rootView.findViewById(R.id.speaker_loudspeaker).asInstanceOf[ImageButton]
    val videoOn = rootView.findViewById(R.id.video_on).asInstanceOf[ImageButton]
    val videoOff = rootView.findViewById(R.id.video_off).asInstanceOf[ImageButton]

    allToggleButtons = List(micOn, micOff, speakerOn, speakerOff, loudspeakerOn, videoOn, videoOff)

    setupOnClickToggle(micOn, micOff, call.muteSelfAudio)
    setupOnClickToggle(micOff, micOn, call.unmuteSelfAudio)

    setupOnClickToggle(loudspeakerOn, speakerOff, call.muteFriendAudio)
    setupOnClickToggle(speakerOff, speakerOn, () => {
      audioManager.setSpeakerphoneOn(false)
      call.unmuteFriendAudio()
    })
    setupOnClickToggle(speakerOn, loudspeakerOn, () => {
      audioManager.setSpeakerphoneOn(true)
    })

    setupOnClickToggle(videoOn, videoOff, call.hideSelfVideo)
    setupOnClickToggle(videoOff, videoOn, call.showSelfVideo)

    allToggleButtons.foreach(_.setEnabled(false))

    val returnToChat = rootView.findViewById(R.id.return_to_chat).asInstanceOf[ImageButton]
    returnToChat.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        val intent = new Intent(getActivity, classOf[ChatActivity])
        intent.setAction(Constants.SWITCH_TO_FRIEND)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY) //stop chats from stacking up
        intent.putExtra("key", activeKey.toString)
        startActivity(intent)

        getActivity.finish()
      }
    })

    durationView = rootView.findViewById(R.id.call_duration).asInstanceOf[Chronometer]

    compositeSubscription +=
      call.ringingObservable.distinctUntilChanged.subscribe(ringing => {
        if (ringing) {
          setupOutgoing()
        } else {
          setupActive()
        }
      })

    rootView
  }

  private def setupOnClickToggle(clickView: View, shownView: View, action: () => Unit): Unit = {
    clickView.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        clickView.setVisibility(View.GONE)
        shownView.setVisibility(View.VISIBLE)

        action()
      }
    })
  }

  def setupOutgoing(): Unit = {
    callStateView.setVisibility(View.VISIBLE)
    callStateView.setText(R.string.call_ringing)
    durationView.setVisibility(View.GONE)
  }

  def setupActive(): Unit = {
    allToggleButtons.foreach(_.setEnabled(true))

    callStateView.setVisibility(View.GONE)

    durationView.setVisibility(View.VISIBLE)
    durationView.setBase(SystemClock.elapsedRealtime() - call.duration.toMillis)
    durationView.start()
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    durationView.stop()
  }
}
