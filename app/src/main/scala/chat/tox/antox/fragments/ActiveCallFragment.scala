package chat.tox.antox.fragments

import android.media.MediaPlayer.OnPreparedListener
import android.media.{AudioManager, MediaPlayer}
import android.os.{Bundle, SystemClock}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{Chronometer, ImageButton}
import chat.tox.antox.R
import chat.tox.antox.av.{Call, OngoingCallNotification}
import chat.tox.antox.data.State
import chat.tox.antox.utils.MediaUtils
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

  var ringbackToneSound: MediaPlayer = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    getActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)

    ringbackToneSound = MediaUtils.setupSound(getActivity, R.raw.ringback_tone, AudioManager.STREAM_VOICE_CALL, looping = true)

    /* Set up the speaker/mic buttons */
    val micOn = rootView.findViewById(R.id.mic_on).asInstanceOf[ImageButton]
    val micOff = rootView.findViewById(R.id.mic_off).asInstanceOf[ImageButton]
    val speakerOn = rootView.findViewById(R.id.speaker_on).asInstanceOf[ImageButton]
    val speakerOff = rootView.findViewById(R.id.speaker_off).asInstanceOf[ImageButton]
    val videoOn = rootView.findViewById(R.id.video_on).asInstanceOf[ImageButton]
    val videoOff = rootView.findViewById(R.id.video_off).asInstanceOf[ImageButton]

    allToggleButtons = List(micOn, micOff, speakerOn, speakerOff, videoOn, videoOff)

    setupOnClickToggle(micOn, micOff, call.muteSelfAudio)
    setupOnClickToggle(micOff, micOn, call.unmuteSelfAudio)

    setupOnClickToggle(speakerOn, speakerOff, call.muteFriendAudio)
    setupOnClickToggle(speakerOff, speakerOn, call.unmuteFriendAudio)

    setupOnClickToggle(videoOn, videoOff, call.hideSelfVideo)
    setupOnClickToggle(videoOff, videoOn, call.showSelfVideo)

    allToggleButtons.foreach(_.setEnabled(false))

    durationView = rootView.findViewById(R.id.call_duration).asInstanceOf[Chronometer]

    compositeSubscription +=
      State.db.friendInfoList
        .subscribe(fi => {
          println("SUBSCRIPTION TRIGGERED")
          for {
            friend <- fi.find(f => f.key == activeKey)
            callNotification <- maybeCallNotification
          } yield {
            callNotification.updateName(friend.getDisplayName)
            callNotification.show()
          }
        })

    println("GOT TO POST COMPOSITE SUBSCRIPTION ADD THING")

    compositeSubscription +=
      call.ringing.distinctUntilChanged.subscribe(ringing => {
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
    ringbackToneSound.setLooping(true)
    ringbackToneSound.setOnPreparedListener(new OnPreparedListener {
      override def onPrepared(mp: MediaPlayer): Unit = {
        ringbackToneSound.start()
      }
    })

    callStateView.setVisibility(View.VISIBLE)
    callStateView.setText(R.string.call_ringing)
    durationView.setVisibility(View.GONE)
  }

  def setupActive(): Unit = {
    ringbackToneSound.stop()

    maybeCallNotification = Some(new OngoingCallNotification(getActivity, activeKey, call))

    allToggleButtons.foreach(_.setEnabled(true))

    callStateView.setVisibility(View.GONE)

    durationView.setVisibility(View.VISIBLE)
    durationView.setBase(SystemClock.elapsedRealtime() - call.duration)
    durationView.start()
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    durationView.stop()
    ringbackToneSound.release()
  }
}
