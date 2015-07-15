package im.tox.antox.activities

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import android.app.Activity
import android.content.Context
import android.hardware.{SensorEvent, Sensor, SensorManager, SensorEventListener}
import android.media.RingtoneManager
import android.os.{PowerManager, Vibrator, Build, Bundle}
import android.util.TypedValue
import android.view.View.OnClickListener
import android.view.ViewGroup.LayoutParams
import android.view.{View, WindowManager}
import android.widget.{FrameLayout, LinearLayout, ImageView, TextView}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.antox.av.Call
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.antox.utils.{IconColor, BitmapManager}
import im.tox.antox.wrapper.{UserStatus, FriendInfo, ToxKey}
import im.tox.antoxnightly.R
import im.tox.tox4j.av.enums.ToxCallState
import rx.lang.scala.{Observable, Subscription}
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.subscriptions.CompositeSubscription

import scala.concurrent.duration._

class CallActivity extends Activity {

  var call: Call = _
  var activeKey: ToxKey = _

  var answerCallButton: View = _
  var endCallButton: View = _
  val callButtonSize = 72 // in dp

  var nameView: TextView = _
  var avatarView: CircleImageView = _
  var durationView: TextView = _

  var vibrator: Vibrator = _
  val vibrationPattern = Array[Long](0, 1000, 1000)

  val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
  val ringtone = RingtoneManager.getRingtone(this, notification)

  private var powerManager: PowerManager = _
  private var maybeWakeLock: Option[PowerManager#WakeLock] = None

  val compositeSubscription = CompositeSubscription()

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

    activeKey = new ToxKey(getIntent.getStringExtra("key"))

    avatarView = findViewById(R.id.avatar).asInstanceOf[CircleImageView]
    nameView = findViewById(R.id.friend_name).asInstanceOf[TextView]

    /* Set up the speaker/mic buttons */
    val micOff = findViewById(R.id.mic_off)
    val micOn = findViewById(R.id.mic_on)
    val speakerOn = findViewById(R.id.speaker_on)
    val speakerOff = findViewById(R.id.speaker_off)
    val videoOn = findViewById(R.id.video_on)
    val videoOff = findViewById(R.id.video_off)

    setupOnClickToggle(micOn, micOff, call.muteSelfAudio)
    setupOnClickToggle(micOff, micOn, call.unmuteSelfAudio)

    setupOnClickToggle(speakerOn, speakerOff, call.muteFriendAudio)
    setupOnClickToggle(speakerOff, speakerOn, call.unmuteFriendAudio)

    setupOnClickToggle(videoOn, videoOff, call.hideSelfVideo)
    setupOnClickToggle(videoOff, videoOn, call.showSelfVideo)

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

    registerSubscriptions()
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

  private def registerSubscriptions(): Unit = {
    compositeSubscription +=
      call.friendStateSubject
        .subscribe(callState => {
        if (callState.contains(ToxCallState.FINISHED)) {
          endCall()
        }
      })

    compositeSubscription +=
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

    // update displayed friend info on change
    compositeSubscription +=
      Reactive.friendInfoList
        .subscribeOn(IOScheduler())
        .observeOn(AndroidMainThreadScheduler())
        .subscribe(fi => {
        updateDisplayedState(fi)
      })

    // updates duration timer every second
    val durationFormat = new SimpleDateFormat("hh:mm:ss")
    compositeSubscription +=
      Observable.interval(1 seconds)
        .subscribeOn(IOScheduler())
        .observeOn(AndroidMainThreadScheduler())
        .map(_ => call.duration)
        .subscribe(duration => {
        durationView.setText(durationFormat.format(new Date(duration)))
      })
  }

  private def updateDisplayedState(fi: Array[FriendInfo]): Unit = {
    val key = activeKey
    val mFriend: Option[FriendInfo] = fi.find(f => f.key == key)

    mFriend match {
      case Some(friend) => {
        nameView.setText(friend.getAliasOrName)

        val avatar = friend.avatar
        avatar.foreach(avatar => {
          BitmapManager.load(avatar, avatarView, isAvatar = true)
        })
      }
      case None =>
        nameView.setText("")
    }
  }

  def setupIncoming(): Unit = {
    // vibrate and ring on incoming call
    vibrator.vibrate(vibrationPattern, 0)
    ringtone.play()
  }

  def setupOutgoing(): Unit = {
    hideAnswerButton()
  }

  def setupActive(): Unit = {
    vibrator.cancel()
    ringtone.stop()
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
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, callButtonSize, getResources.getDisplayMetrics).asInstanceOf[Int], 2f)

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
    finish()
  }

  override def onPause(): Unit = {
    maybeWakeLock.foreach(wakeLock => {
      if (wakeLock.isHeld) {
        wakeLock.release()
      }
    })
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    compositeSubscription.unsubscribe()
    vibrator.cancel()
    ringtone.stop()
  }
}
