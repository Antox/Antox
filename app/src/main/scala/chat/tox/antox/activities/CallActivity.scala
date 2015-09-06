package chat.tox.antox.activities

import java.text.SimpleDateFormat
import java.util.Date

import android.app.Activity
import android.content.{Context, IntentFilter}
import android.media.{AudioManager, MediaPlayer}
import android.os.{Build, Bundle, PowerManager, Vibrator}
import android.provider.Settings
import android.util.TypedValue
import android.view.View.OnClickListener
import android.view.{View, WindowManager}
import android.widget.{LinearLayout, TextView}
import chat.tox.antox.R
import chat.tox.antox.av.{Call, OngoingCallNotification}
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{BitmapManager, RxAndroid}
import chat.tox.antox.wrapper.{FriendInfo, ToxKey}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.tox4j.av.enums.ToxavFriendCallState
import rx.lang.scala.Observable
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

  var audioManager: AudioManager = _
  var ringtone: MediaPlayer = _
  var ringerMode: Observable[Int] = _

  var callNotification: OngoingCallNotification = _

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
      flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
    }

    getWindow.addFlags(flags)

    setContentView(R.layout.activity_call)
    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)

    vibrator = this.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]

    // power manager used to turn off screen when the proximity sensor is triggered
    powerManager = getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager]
    if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
      maybeWakeLock = Some(powerManager.newWakeLock(
        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
        "im.tox.antox.CallActivity"))
    }

    activeKey = new ToxKey(getIntent.getStringExtra("key"))
    val friend = ToxSingleton.getAntoxFriend(activeKey).get
    call = friend.call

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

    durationView = findViewById(R.id.duration).asInstanceOf[TextView]

    callNotification = new OngoingCallNotification(this, friend, call)
    callNotification.updateName("LET US TEST THIS")
    callNotification.show()

    ringtone = new MediaPlayer()
    ringtone.setDataSource(this, Settings.System.DEFAULT_RINGTONE_URI)
    audioManager = getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
    if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
      ringtone.setAudioStreamType(AudioManager.STREAM_RING)
      ringtone.setLooping(true)
      ringtone.prepare()
    }

    ringerMode = RxAndroid
      .fromBroadcast(this, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
      .map(_.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1))

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
        if (callState.contains(ToxavFriendCallState.FINISHED)) {
          endCall()
        }
      })

    compositeSubscription +=
      call.ringing.distinctUntilChanged.subscribe(ringing => {
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

    compositeSubscription +=
      ringerMode.subscribe(mode => {
        println("got stream type change")
        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
          //ringtone.start()
        }

        if (audioManager.getRingerMode != AudioManager.RINGER_MODE_SILENT) {
          //vibrator.vibrate(vibrationPattern, 0)
        }
      })

    // update displayed friend info on change
    compositeSubscription +=
      State.db.friendInfoList
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

  private def updateDisplayedState(fi: Seq[FriendInfo]): Unit = {
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
    ringtone.start()
    vibrator.vibrate(vibrationPattern, 0)
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
    super.onPause()

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
