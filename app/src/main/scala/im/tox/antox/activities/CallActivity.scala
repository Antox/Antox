package im.tox.antox.activities

import android.app.Activity
import android.content.Context
import android.hardware.{SensorEvent, Sensor, SensorManager, SensorEventListener}
import android.os.{PowerManager, Vibrator, Build, Bundle}
import android.util.TypedValue
import android.view.View.OnClickListener
import android.view.ViewGroup.LayoutParams
import android.view.{View, WindowManager}
import android.widget.{FrameLayout, LinearLayout, ImageView, TextView}
import im.tox.antox.av.Call
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.BitmapManager
import im.tox.antox.wrapper.ToxKey
import im.tox.antoxnightly.R
import im.tox.tox4j.av.enums.ToxCallState
import rx.lang.scala.Subscription


class CallActivity extends Activity {

  var call: Call = _
  var stateSub: Subscription = _

  var answerCallButton: View = _
  var endCallButton: View = _
  val callButtonSize = 72 // in dp

  var vibrator: Vibrator = _
  val vibrationPattern = Array[Long](0, 1000, 1000)

  private var powerManager: PowerManager = _
  private var maybeWakeLock: Option[PowerManager#WakeLock] = None

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    var flags: Int =
      // set this flag so this activity will stay in front of the keyguard
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
      // Have the WindowManager filter out touch events that are "too fat".
      WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES

    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      flags = flags | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
    }

    getWindow.addFlags(flags)

    setContentView(R.layout.activity_call)

    vibrator = this.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]

    // power manager used to turn off screen when the proximity sensor is triggered
    powerManager = getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager]
    if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
      maybeWakeLock = Some(powerManager.newWakeLock(
        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
        "im.tox.antox.CallActivity"))
    }

    /* Set up avatar and name */
    val name = findViewById(R.id.friend_name).asInstanceOf[TextView]
    name.setText(getIntent.getStringExtra("name"))

    val key = new ToxKey(getIntent.getStringExtra("key"))
    if (key != null) { // Will happen when you attempt to call a user you have added but hasn't come online yet
      val friend = ToxSingleton.getAntoxFriend(key)
      friend.foreach(friend => {
        call = friend.call
        friend.getAvatar.foreach(avatar => {
          val avatarView = findViewById(R.id.avatar).asInstanceOf[ImageView]
          BitmapManager.load(avatar, avatarView, isAvatar = true)
        })
      })
    }

    /* Set up the volume/mic buttons */
    val micOff = findViewById(R.id.mic_off)
    val micOn = findViewById(R.id.mic_on)
    val volumeOn = findViewById(R.id.volume_on)
    val volumeOff = findViewById(R.id.volume_off)
    val videoOn = findViewById(R.id.video_on)
    val videoOff = findViewById(R.id.video_off)

    micOff.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        micOff.setVisibility(View.GONE)
        micOn.setVisibility(View.VISIBLE)

        call.unmuteSelfAudio()
      }
    })

    micOn.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        micOff.setVisibility(View.VISIBLE)
        micOn.setVisibility(View.GONE)

        call.muteSelfAudio()
      }
    })

    volumeOn.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        volumeOff.setVisibility(View.VISIBLE)
        volumeOn.setVisibility(View.GONE)

        call.muteFriendAudio()
      }
    })

    volumeOff.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        volumeOff.setVisibility(View.GONE)
        volumeOn.setVisibility(View.VISIBLE)

        call.unmuteFriendAudio()
      }
    })

    videoOn.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        videoOff.setVisibility(View.GONE)
        videoOn.setVisibility(View.VISIBLE)

        call.showSelfVideo()
      }
    })


    videoOff.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        videoOn.setVisibility(View.GONE)
        videoOff.setVisibility(View.VISIBLE)

        call.hideSelfVideo()
      }
    })

    /* Set up the answer and av buttons */
    answerCallButton = findViewById(R.id.answer_call_circle)

    answerCallButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.answerCall(receivingAudio = true, receivingVideo = false) //TODO FIXME HELP
      }
    })


    /* Set up the end call and av buttons */
    endCallButton = findViewById(R.id.end_call_circle)

    endCallButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.end()

        endCall()
      }
    })
  }

  def onSensorChanged(event: SensorEvent) {
    val distance: Float = event.values(0)
  }

  override def onResume(): Unit = {
    super.onResume()

    stateSub = call.friendStateSubject
      .subscribe(callState => {
      if (callState.contains(ToxCallState.FINISHED)) {
        endCall()
      }
    })

    call.ringing.subscribe(ringing => {
      if (ringing) {
        if (call.incoming) {
          setupIncoming()
        } else {
          setupOutgoing()
        }
      } else {
        setupActive()
      }
    })
  }

  override def onPause(): Unit = {
    super.onPause()

    stateSub.unsubscribe()
  }

  def setupIncoming(): Unit = {
    // vibrate on incoming call
    vibrator.vibrate(vibrationPattern, 0)
  }

  def setupOutgoing(): Unit = {
    hideAnswerButton()
  }

  def setupActive(): Unit = {
    vibrator.cancel()
    hideAnswerButton()

    maybeWakeLock.foreach(wakeLock => {
      if (!wakeLock.isHeld) {
        wakeLock.acquire()
      }
    })
  }

  def hideAnswerButton(): Unit = {
    findViewById(R.id.answer_call_frame).setVisibility(View.GONE)

    // hack to reset the end call button to
    // its original size and center it in the call view
    val lp = new LinearLayout.LayoutParams(
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, callButtonSize, getResources.getDisplayMetrics).asInstanceOf[Int],
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, callButtonSize, getResources.getDisplayMetrics).asInstanceOf[Int], 1f)

    findViewById(R.id.end_call_frame).setLayoutParams(lp)
  }

  def setupOnHold(): Unit = {
    //NA TODO
  }

  def hideViewOnHold(): Unit = {
    //NA TODO
  }

  // Called when the call ends (both by the user and by friend)
  def endCall(): Unit = {
    maybeWakeLock.foreach(wakeLock => {
      if (wakeLock.isHeld) {
        wakeLock.release()
      }
    })

    finish()
  }

  override def onDestroy(): Unit = {
    vibrator.cancel()
  }
}

