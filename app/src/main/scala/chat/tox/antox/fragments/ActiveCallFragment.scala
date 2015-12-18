package chat.tox.antox.fragments

import java.util.concurrent.TimeUnit

import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.{Bundle, SystemClock}
import android.view.View.OnClickListener
import android.view._
import android.view.animation.Animation.AnimationListener
import android.view.animation.{AccelerateInterpolator, AlphaAnimation, Animation}
import android.widget.{Chronometer, FrameLayout}
import chat.tox.antox.R
import chat.tox.antox.activities.ChatActivity
import chat.tox.antox.av.{Call, VideoDisplay}
import chat.tox.antox.utils.Constants
import chat.tox.antox.utils.ObservableExtensions.RichObservable
import chat.tox.antox.wrapper.ContactKey
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.NewThreadScheduler

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

  var videoSurface: TextureView = _
  var videoDisplay: Option[VideoDisplay] = None

  var viewsHiddenOnFade: List[View] = _
  val fadeDelay = Duration(4, TimeUnit.SECONDS)
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

    videoSurface = rootView.findViewById(R.id.video_surface).asInstanceOf[TextureView]
    videoDisplay = Some(new VideoDisplay(call.videoFrameObservable, videoSurface, call.videoBufferLength))

    compositeSubscription +=
      call.ringingObservable
        .combineLatest(call.selfStateObservable)
        .sub { case (ringing, selfState) =>
          if (call.active) {
            if (ringing) {
              setupOutgoing()
            } else {
              setupActive()
            }

            setupVideoUi(selfState.receivingVideo, selfState.sendingVideo)
          }
        }

    viewsHiddenOnFade = List(buttonsView, upperCallHalfView)
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
      avatarView.setVisibility(View.GONE)
      nameView.setTextColor(getActivity.getResources.getColor(R.color.white))
      durationView.setTextColor(getActivity.getResources.getColor(R.color.white))

      videoDisplay.foreach(_.start())

      // fade out when the video view hasn't been clicked in a while
      startUiFadeTimer()
      videoSurface.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = {
          lastClickTime = System.currentTimeMillis()
          viewsHiddenOnFade.foreach(view => {
            view.setVisibility(View.VISIBLE)
            view.clearAnimation()
            view.animate().cancel()
          })
          startUiFadeTimer()
        }
      })
    } else if(!videoEnabled) {
      getActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
      avatarView.setVisibility(View.VISIBLE)

      videoDisplay.foreach(_.stop())
      nameView.setTextColor(getActivity.getResources.getColor(R.color.grey_darkest))
      durationView.setTextColor(getActivity.getResources.getColor(R.color.black))
    }
  }

  def startUiFadeTimer(): Unit = {
    compositeSubscription +=
      Observable
        .timer(fadeDelay)
        .subscribeOn(NewThreadScheduler())
        .flatMap(_ => call.callVideoObservable)
        .sub(video => {
          val timeSinceLastClick = System.currentTimeMillis() - lastClickTime
          if (call.active && !call.ringing && timeSinceLastClick >= fadeDelay.toMillis && video) {
            val fadeOut = new AlphaAnimation(1, 0)
            fadeOut.setInterpolator(new AccelerateInterpolator())
            fadeOut.setDuration(2500)
            fadeOut.setAnimationListener(new AnimationListener {
              override def onAnimationEnd(animation: Animation): Unit = {
                viewsHiddenOnFade.foreach(_.setVisibility(View.GONE))
              }

              override def onAnimationStart(animation: Animation): Unit = {}

              override def onAnimationRepeat(animation: Animation): Unit = {}
            })

            viewsHiddenOnFade.foreach(view => {
              view.setAnimation(fadeOut)
              view.animate()
            })
          }
        })
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    videoDisplay.foreach(_.stop())
    durationView.stop()
  }
}
