package chat.tox.antox.activities

import android.content.Context
import android.media.AudioManager
import android.os.{Build, Bundle, PowerManager}
import android.support.v4.app.FragmentActivity
import android.view.WindowManager
import chat.tox.antox.R
import chat.tox.antox.av.Call
import chat.tox.antox.data.State
import chat.tox.antox.fragments.{ActiveCallFragment, IncomingCallFragment}
import chat.tox.antox.utils._
import chat.tox.antox.wrapper._
import rx.lang.scala.subscriptions.CompositeSubscription

import scala.language.postfixOps

class CallActivity extends FragmentActivity {

  var call: Call = _
  var activeKey: ContactKey = _

  private var powerManager: PowerManager = _
  private var maybeWakeLock: Option[PowerManager#WakeLock] = None

  val compositeSubscription = CompositeSubscription()

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    var windowFlags: Int =
      // set this flag so this activity will stay in front of the keyguard
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
      // Have the WindowManager filter out touch events that are "too fat".
      WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES

    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      windowFlags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
    }

    getWindow.addFlags(windowFlags)

    setContentView(R.layout.activity_call)
    setVolumeControlStream(AudioManager.STREAM_RING)

    // power manager used to turn off screen when the proximity sensor is triggered
    powerManager = getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager]
    if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
      maybeWakeLock = Some(powerManager.newWakeLock(
        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
        AntoxLog.DEFAULT_TAG.toString))
    }

    activeKey = new FriendKey(getIntent.getStringExtra("key"))
    val callNumber = CallNumber(getIntent.getIntExtra("call_number", -1))
    call = State.callManager.get(callNumber).getOrElse(throw new IllegalStateException("Call number is required."))

    registerSubscriptions()
  }

  private def registerSubscriptions(): Unit = {
    compositeSubscription +=
      call.ringing.distinctUntilChanged.subscribe(ringing => {
        if (ringing && call.incoming) {
          val fragmentTransaction = getSupportFragmentManager.beginTransaction()
          fragmentTransaction.add(R.id.call_fragment_container, new IncomingCallFragment(call, activeKey))
          fragmentTransaction.commit()
        } else {
          val fragmentTransaction = getSupportFragmentManager.beginTransaction()
          fragmentTransaction.replace(R.id.call_fragment_container, new ActiveCallFragment(call, activeKey))
          fragmentTransaction.commit()
        }
      })
  }

  def setupOnHold(): Unit = {
    //NA TODO
  }

  def hideViewOnHold(): Unit = {
    //NA TODO
  }

  override def onResume(): Unit = {
    super.onResume()

    maybeWakeLock.foreach(wakeLock => {
      if (!wakeLock.isHeld) {
        wakeLock.acquire()
      }
    })
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
  }
}
