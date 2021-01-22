package chat.tox.antox.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.{Build, Bundle}
import android.support.v4.app.FragmentActivity
import android.view.{View, ViewAnimationUtils, ViewTreeObserver, WindowManager}
import android.widget.FrameLayout
import chat.tox.antox.R
import chat.tox.antox.av.Call
import chat.tox.antox.data.State
import chat.tox.antox.fragments.{ActiveCallFragment, IncomingCallFragment}
import chat.tox.antox.tox.MessageHelper
import chat.tox.antox.utils.ObservableExtensions.RichObservable
import chat.tox.antox.utils._
import chat.tox.antox.wrapper._
import im.tox.tox4j.core.enums.ToxMessageType
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.subscriptions.CompositeSubscription

import scala.language.postfixOps

class CallActivity extends FragmentActivity with CallReplySelectedListener {

  var call: Call = _
  var activeKey: ContactKey = _

  private val compositeSubscription = CompositeSubscription()

  private var rootLayout: FrameLayout = _

  protected override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    overridePendingTransition(0, 0)

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

    activeKey = FriendKey(getIntent.getStringExtra("key"))
    val callNumber = CallNumber(getIntent.getIntExtra("call_number", -1))
    call =
      State.callManager.get(callNumber).getOrElse({
        AntoxLog.debug(s"Ending call which has an invalid call number $callNumber")
        finish()
        return
      })

    if (getIntent.getAction == Constants.END_CALL) {
      call.end()
      finish()
    }

    compositeSubscription +=
      call.videoEnabledObservable
        .combineLatest(call.ringingObservable)
        .sub { case (videoEnabled, ringing) =>
          if (!ringing && videoEnabled) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
          } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
          }
        }

    val clickLocation = Option(getIntent.getExtras.get("click_location").asInstanceOf[Location])

    rootLayout = findViewById(R.id.call_fragment_container).asInstanceOf[FrameLayout]
    if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      rootLayout.setVisibility(View.INVISIBLE)

      val viewTreeObserver = rootLayout.getViewTreeObserver
      if (viewTreeObserver.isAlive) {
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          override def onGlobalLayout(): Unit = {
            circularRevealActivity(clickLocation)

            rootLayout.getViewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        })
      }
    }

    setVolumeControlStream(AudioManager.STREAM_RING)

    registerSubscriptions()
  }

  def circularRevealActivity(maybeClickLocation: Option[Location]): Unit = {
    val cx = maybeClickLocation.map(_.x).getOrElse(rootLayout.getWidth / 2)
    val cy = maybeClickLocation.map(_.y).getOrElse(rootLayout.getHeight / 2)

    val finalRadius = Math.max(rootLayout.getWidth, rootLayout.getHeight)

    // create the animator for this view (the start radius is zero)
    val circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, cx, cy, 0, finalRadius)
    circularReveal.setDuration(300)

    // make the view visible and start the animation
    rootLayout.setVisibility(View.VISIBLE)
    circularReveal.start()
  }

  private def registerSubscriptions(): Unit = {
    compositeSubscription +=
      call.endedObservable.observeOn(AndroidMainThreadScheduler()).subscribe(_ => {
        onCallEnded()
      })

    compositeSubscription +=
      call.ringingObservable.distinctUntilChanged.subscribe(ringing => {
        if (ringing && call.incoming) {
          val fragmentTransaction = getSupportFragmentManager.beginTransaction()
          fragmentTransaction.add(R.id.call_fragment_container, IncomingCallFragment.newInstance(call, activeKey))
          fragmentTransaction.commit()
        } else {
          val fragmentTransaction = getSupportFragmentManager.beginTransaction()
          fragmentTransaction.replace(R.id.call_fragment_container, ActiveCallFragment.newInstance(call, activeKey))
          fragmentTransaction.commit()
        }
      })
  }

  // Called when the call ends (both by the user and by friend)
  def onCallEnded(): Unit = {
    AntoxLog.debug(s"${this.getClass.getSimpleName} on call ended called")
    finish()
  }

  def setupOnHold(): Unit = {
    //N/A TODO
  }

  def hideViewOnHold(): Unit = {
    //N/A TODO
  }

  override def onCallReplySelected(maybeReply: Option[String]): Unit = {
    maybeReply match {
      case Some(reply) =>
        //FIXME when group calls are implemented
        MessageHelper.sendMessage(this, activeKey.asInstanceOf[FriendKey], reply, ToxMessageType.NORMAL, None)

      case None =>
        val intent = AntoxNotificationManager.createChatIntent(this, Constants.SWITCH_TO_FRIEND, classOf[ChatActivity], activeKey)
        startActivity(intent)
    }

    call.end()
    finish()
  }

  override def onNewIntent(intent: Intent): Unit = {
    super.onNewIntent(intent)
    if (intent.getAction == Constants.END_CALL) {
      call.end()
    }
  }

  override def onResume(): Unit = {
    super.onResume()
  }

  override def onPause(): Unit = {
    super.onPause()
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    compositeSubscription.unsubscribe()
  }

  override def onBackPressed(): Unit = {
    if (call.active && call.ringing) {
      call.end()
    }
    super.onBackPressed()
  }
}
