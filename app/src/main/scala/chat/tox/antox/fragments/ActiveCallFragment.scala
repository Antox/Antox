package chat.tox.antox.fragments

import java.util.concurrent.TimeUnit

import android.content.Intent
import android.media.AudioManager
import android.os.{Bundle, SystemClock}
import android.view.View.{OnClickListener, OnTouchListener}
import android.view._
import android.view.animation.Animation.AnimationListener
import android.view.animation.{AccelerateInterpolator, AlphaAnimation, Animation}
import android.widget.{Chronometer, FrameLayout, ImageView}
import chat.tox.antox.R
import chat.tox.antox.activities.ChatActivity
import chat.tox.antox.av.CameraFacing.CameraFacing
import chat.tox.antox.av._
import chat.tox.antox.utils.{AntoxLog, Constants, Options, UiUtils}
import chat.tox.antox.utils.ObservableExtensions.RichObservable
import chat.tox.antox.wrapper.ContactKey
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, NewThreadScheduler}

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

  var backgroundView: View = _
  var durationView: Chronometer = _

  var buttonsView: View = _
  var allButtons: List[FrameLayout] = _

  var videoSurface: TextureView = _
  var videoDisplay: Option[VideoDisplay] = None

  var cameraPreviewSurface: TextureView = _
  var maybeCameraDisplay: Option[CameraDisplay] = None
  var cameraSwapView: ImageView = _

  var viewsHiddenOnFade: List[View] = _
  val fadeDelay = Duration(4, TimeUnit.SECONDS)
  var lastClickTime = System.currentTimeMillis()

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    getActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)

    backgroundView = rootView.findViewById(R.id.call_background)

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

    setupOnClickToggle(micOn, call.muteSelfAudio)
    setupOnClickToggle(micOff, call.unmuteSelfAudio)

    setupOnClickToggle(loudspeakerOn, {
      call.disableLoudspeaker()
      call.muteFriendAudio
    })
    setupOnClickToggle(speakerOff, {
      call.disableLoudspeaker()
      call.unmuteFriendAudio
    })
    setupOnClickToggle(speakerOn, () => {
      call.enableLoudspeaker()
    })

    compositeSubscription +=
      call.selfStateObservable.subscribe(selfState => {
        if (selfState.audioMuted) {
          UiUtils.toggleViewVisibility(micOff, micOn)
        } else {
          UiUtils.toggleViewVisibility(micOn, micOff)
        }

        if (selfState.loudspeakerEnabled) {
          audioManager.setSpeakerphoneOn(true)
        } else {
          audioManager.setSpeakerphoneOn(false)
        }

        if (selfState.loudspeakerEnabled) {
          UiUtils.toggleViewVisibility(loudspeakerOn, speakerOn, speakerOff)
        } else if (selfState.receivingAudio) {
          UiUtils.toggleViewVisibility(speakerOn, speakerOff, loudspeakerOn)
        } else {
          UiUtils.toggleViewVisibility(speakerOff, loudspeakerOn, speakerOn)
        }

        if (selfState.sendingVideo) {
          UiUtils.toggleViewVisibility(videoOn, videoOff)
        } else {
          UiUtils.toggleViewVisibility(videoOff, videoOn)
        }

        if (selfState.videoHidden) {
          UiUtils.toggleViewVisibility(videoOff, videoOn)
        } else {
          UiUtils.toggleViewVisibility(videoOn, videoOff)
        }
      })

    // don't let the user enable video if the device doesn't have a camera
    if (CameraUtils.deviceHasCamera(getActivity)) {
      setupOnClickToggle(videoOn, call.hideSelfVideo)
      setupOnClickToggle(videoOff, call.showSelfVideo)
    }

    setupOnClickToggle(returnToChat, () => {
      val intent = new Intent(getActivity, classOf[ChatActivity])
      intent.setAction(Constants.SWITCH_TO_FRIEND)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY) //stop chats from stacking up
      intent.putExtra("key", activeKey.toString)
      startActivity(intent)

      getActivity.finish()
    })

    if (CameraUtils.deviceHasCamera(getActivity)) {
      if (Options.videoCallStartWithNoVideo) {
        // start with video off!
        call.hideSelfVideo()
      }
    }

    durationView = rootView.findViewById(R.id.call_duration).asInstanceOf[Chronometer]
    videoSurface = rootView.findViewById(R.id.video_surface).asInstanceOf[TextureView]

    // -- crash --
    // -- crash --
    // -- crash --
    // durationView = rootView.findViewById(23398280).asInstanceOf[Chronometer]
    // videoSurface = null
    // durationView = null
    // throw new RuntimeException("whatever")
    // -- crash --
    // -- crash --
    // -- crash --

    val cameraPreviewWrapper = rootView.findViewById(R.id.camera_preview_wrapper).asInstanceOf[FrameLayout]
    cameraPreviewWrapper.setOnTouchListener(new OnTouchListener() {
      override def onTouch(view: View, event: MotionEvent): Boolean = {

        val params = view.getLayoutParams.asInstanceOf[FrameLayout.LayoutParams]
        if (view.getId != R.id.camera_preview_wrapper) return false

        event.getAction match {
          case MotionEvent.ACTION_MOVE | MotionEvent.ACTION_UP =>

            val newX = (event.getRawX - (view.getWidth / 2)).toInt
            val newY = (event.getRawY - view.getHeight).toInt

            params.leftMargin = newX
            params.topMargin = newY

            view.setLayoutParams(params)

          case MotionEvent.ACTION_DOWN =>
            view.setLayoutParams(params)

          case _ => // do nothing
        }

        true
      }
    })

    cameraPreviewSurface = rootView.findViewById(R.id.camera_preview_surface).asInstanceOf[TextureView]
    scaleSurfaceRelativeToScreen(cameraPreviewSurface, 0.3f)

    videoDisplay = Some(new VideoDisplay(getActivity, call.videoFrameObservable, videoSurface, call.videoBufferLength))
    val cameraDisplay = new CameraDisplay(getActivity, cameraPreviewSurface, cameraPreviewWrapper, buttonsView.getHeight)
    maybeCameraDisplay = Some(cameraDisplay)

    cameraSwapView = rootView.findViewById(R.id.swap_camera).asInstanceOf[ImageView]
    cameraSwapView.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        call.rotateCamera()
      }
    })

    compositeSubscription +=
      call.cameraFacingObservable.sub {
        case CameraFacing.Front =>
          cameraSwapView.setImageResource(R.drawable.ic_camera_front_white_24dp)
        case CameraFacing.Back =>
          cameraSwapView.setImageResource(R.drawable.ic_camera_rear_white_24dp)
      }

    compositeSubscription +=
      call.ringingObservable
        .distinctUntilChanged
        .observeOn(AndroidMainThreadScheduler())
        .sub { case (ringing) =>
          if (call.active) {
            if (ringing) {
              setupOutgoing()
            } else {
              setupActive()
            }
          }
        }

    compositeSubscription +=
      call.ringingObservable
        .combineLatest(call.selfStateObservable)
        .map(t => (t._1, t._2.receivingVideo, t._2.sendingVideo))
        .distinctUntilChanged
        .observeOn(AndroidMainThreadScheduler())
        .sub { case (ringing, receivingVideo, sendingVideo) =>
          if (call.active && !call.ringing) {
            setupIncomingVideoUi(receivingVideo, sendingVideo)
          }
        }

    compositeSubscription +=
      call.ringingObservable
        .combineLatest(call.selfStateObservable)
        .map(t => (t._1, t._2.sendingVideo))
        .combineLatestWith(call.cameraFacingObservable)((t, facing) => (t._1, t._2, facing))
        .distinctUntilChanged
        .observeOn(AndroidMainThreadScheduler())
        .sub { case (ringing, sendingVideo, facing) =>
          if (call.active && !call.ringing) {
            setupOutgoingVideoUi(sendingVideo, facing)
          }
        }

    viewsHiddenOnFade = List(buttonsView, upperCallHalfView)
    rootView
  }

  /**
    * Scales a [[TextureView]] to a size relative to that of the screen.
    *
    * @param textureView [[TextureView]] to be scaled
    * @param scale       amount to multiply the width and height of the screen by to get the new size
    */
  private def scaleSurfaceRelativeToScreen(textureView: TextureView, scale: Float): Unit = {
    val layoutParams = textureView.getLayoutParams

    layoutParams.width = (UiUtils.getScreenWidth(getActivity) * scale).toInt
    layoutParams.height = (UiUtils.getScreenHeight(getActivity) * scale).toInt

    textureView.setLayoutParams(layoutParams)
  }

  private def setupOnClickToggle(clickView: View, action: () => Unit): Unit = {
    clickView.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (call.active && !call.ringing)
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

  def setupIncomingVideoUi(receivingVideo: Boolean, sendingVideo: Boolean): Unit = {
    val videoEnabled: Boolean = receivingVideo || sendingVideo
    if (videoEnabled) {
      backgroundView.setBackgroundColor(getResources.getColor(R.color.black_absolute))

      avatarView.setVisibility(View.GONE)
      nameView.setTextColor(getActivity.getResources.getColor(R.color.white))
      durationView.setTextColor(getActivity.getResources.getColor(R.color.white))

      // turn on loudspeaker in a video call
      if (!audioManager.isWiredHeadsetOn) call.enableLoudspeaker()

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
    } else if (!videoEnabled) {
      backgroundView.setBackgroundColor(getResources.getColor(R.color.white))
      avatarView.setVisibility(View.VISIBLE)

      videoDisplay.foreach(_.stop())

      nameView.setTextColor(getActivity.getResources.getColor(R.color.grey_darkest))
      durationView.setTextColor(getActivity.getResources.getColor(R.color.black))
    }
  }

  def setupOutgoingVideoUi(sendingVideo: Boolean, cameraFacing: CameraFacing): Unit = {
    AntoxLog.debug("stopping some video stuff")

    if (sendingVideo) {
      // get preferred camera or default camera or exit and disable video on failure
      // (there will always be some camera or sendingVideo would be false)
      val camera = CameraUtils.getCameraInstance(cameraFacing)
        .getOrElse(CameraUtils.getCameraInstance(CameraFacing.Back).getOrElse({
          call.hideSelfVideo()
          AntoxLog.debug("hiding self video because camera could not be accessed")
          return
        }))

      CameraUtils.setCameraDisplayOrientation(getActivity, camera)

      cameraPreviewSurface.setVisibility(View.VISIBLE)

      call.cameraFrameBuffer = maybeCameraDisplay.map(_.frameBuffer)
      maybeCameraDisplay.foreach(_.start(camera))
    } else {
      AntoxLog.debug("stopping video stuff")

      call.cameraFrameBuffer = None
      maybeCameraDisplay.foreach(_.stop())

      cameraPreviewSurface.setVisibility(View.GONE)
    }
  }

  def startUiFadeTimer(): Unit = {
    compositeSubscription +=
      Observable
        .timer(fadeDelay)
        .subscribeOn(NewThreadScheduler())
        .flatMap(_ => call.videoEnabledObservable)
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
    maybeCameraDisplay.foreach(_.stop())
    call.cameraFrameBuffer = None

    durationView.stop()
  }
}
