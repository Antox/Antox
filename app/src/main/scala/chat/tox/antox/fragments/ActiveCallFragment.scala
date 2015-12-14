package chat.tox.antox.fragments

import java.util.concurrent.TimeUnit

import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.{Bundle, SystemClock}
import android.view.View.OnClickListener
import android.view.animation.{AccelerateInterpolator, AlphaAnimation}
import android.view.{SurfaceView, LayoutInflater, View, ViewGroup}
import android.widget.{FrameLayout, Chronometer, ImageButton}
import chat.tox.antox.R
import chat.tox.antox.activities.ChatActivity
import chat.tox.antox.av.{VideoDisplay, Call}
import chat.tox.antox.utils.Constants
import chat.tox.antox.utils.ObservableExtensions.RichObservable
import chat.tox.antox.wrapper.ContactKey
import rx.lang.scala.Observable

import scala.concurrent.duration.Duration

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

  var buttonsView: View = _
  var allButtons: List[FrameLayout] = _

  var videoSurface: SurfaceView = _
  var videoDisplay: Option[VideoDisplay] = None

  val fadeDelay = Duration(5, TimeUnit.SECONDS)
  var lastClickTime = System.currentTimeMillis()

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    getActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)

    /* Set up the speaker/mic buttons */
    buttonsView = rootView.findViewById(R.id.call_buttons)
    val micOn = rootView.findViewById(R.id.mic_on).asInstanceOf[FrameLayout]
    val micOff = rootView.findViewById(R.id.mic_off).asInstanceOf[FrameLayout]
    val speakerOn = rootView.findViewById(R.id.speaker_on).asInstanceOf[FrameLayout]
    val speakerOff = rootView.findViewById(R.id.speaker_off).asInstanceOf[FrameLayout]
    val loudspeakerOn = rootView.findViewById(R.id.speaker_loudspeaker).asInstanceOf[FrameLayout]
    val videoOn = rootView.findViewById(R.id.video_on).asInstanceOf[FrameLayout]
    val videoOff = rootView.findViewById(R.id.video_off).asInstanceOf[FrameLayout]

    val returnToChat = rootView.findViewById(R.id.return_to_chat).asInstanceOf[FrameLayout]

    allButtons = List(micOn, micOff, speakerOn, speakerOff, loudspeakerOn, videoOn, videoOff, returnToChat)
    allButtons.foreach(_.setClickable(false))

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

    videoSurface = rootView.findViewById(R.id.video_surface).asInstanceOf[SurfaceView]
    videoDisplay = Some(new VideoDisplay(call, videoSurface))

    compositeSubscription +=
      call.ringingObservable
        .combineLatest(call.callVideoObservable)
        .sub { case (ringing, (receivingVideo, sendingVideo)) =>
          if (ringing) {
            setupOutgoing()
          } else {
            setupActive()
          }

          setupVideoUi(receivingVideo, sendingVideo)
        }

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
    allButtons.foreach(_.setClickable(true))

    callStateView.setVisibility(View.GONE)

    durationView.setVisibility(View.VISIBLE)
    durationView.setBase(SystemClock.elapsedRealtime() - call.duration.toMillis)
    durationView.start()
  }

  def setupVideoUi(receivingVideo: Boolean, sendingVideo: Boolean): Unit = {
    val videoEnabled: Boolean = receivingVideo || sendingVideo
    if (videoEnabled) {
      getActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER)
      videoSurface.setVisibility(View.VISIBLE)
      avatarView.setVisibility(View.GONE)

      // fade out when the video view hasn't been clicked in a while
      videoSurface.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = {
          lastClickTime = System.currentTimeMillis()

        }
      })
    } else if(!videoEnabled) {
      getActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
      videoSurface.setVisibility(View.GONE)
      avatarView.setVisibility(View.VISIBLE)
    }
  }

  def startUiFadeTimer(): Unit = {
    Observable
      .timer(fadeDelay).flatMap(_ => call.callVideoObservable).foreach(t => {
      if (!t._1 && !t._2) return // return if video is no longer enabled

      val timeSinceLastClick = System.currentTimeMillis() - lastClickTime
      if (call.active && !call.ringing && timeSinceLastClick >= fadeDelay.toMillis) {
        val fadeOut = new AlphaAnimation(1, 0)
        fadeOut.setInterpolator(new AccelerateInterpolator())
        fadeOut.setDuration(1000)

        List(buttonsView, upperCallHalfView).foreach(view => {
          view.setAnimation(fadeOut)
          view.animate()
        })
      }
    })
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    videoDisplay.foreach(_.destroy())
    durationView.stop()
  }
}
