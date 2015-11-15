package chat.tox.antox.fragments

import android.media.AudioManager
import android.os.{SystemClock, Bundle}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{Chronometer, ImageButton}
import chat.tox.antox.R
import chat.tox.antox.av.{Call, OngoingCallNotification}
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.ContactKey
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

class ActiveCallFragment(call: Call, activeKey: ContactKey) extends CommonCallFragment(call, activeKey, R.layout.fragment_call) {

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

    rootView
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)

    compositeSubscription +=
      State.db.friendInfoList
        .subscribeOn(IOScheduler())
        .observeOn(AndroidMainThreadScheduler())
        .subscribe(fi => {
          for {
            friend <- fi.find(f => f.key == activeKey)
            callNotification <- maybeCallNotification
          } yield {
            callNotification.updateName(friend.getDisplayName)
            callNotification.show()
          }
        })

    compositeSubscription +=
      call.ringing.distinctUntilChanged.subscribe(ringing => {
        if (ringing) {
          setupOutgoing()
        } else {
          setupActive()
        }
      })
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
    maybeRingtone.foreach(_.stop())

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
  }
}
