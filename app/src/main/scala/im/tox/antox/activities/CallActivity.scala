package im.tox.antox.activities

import android.app.Activity
import android.content.Context
import android.os.{Vibrator, Build, Bundle}
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

  var vibrator: Vibrator = _

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

        call.muteSelfAudio()
      }
    })

    micOn.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        if (!call.active) return

        micOff.setVisibility(View.VISIBLE)
        micOn.setVisibility(View.GONE)

        call.unmuteSelfAudio()
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
    answerCallButton = findViewById(R.id.answerCallButton)

    answerCallButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.answerCall(receivingAudio = true, receivingVideo = false) //TODO FIXME HELP

        endCall()
      }
    })


    /* Set up the end call and av buttons */
    endCallButton = findViewById(R.id.endCallButton)

    endCallButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.end()

        endCall()
      }
    })

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
          setupViewIncoming()
        } else {
          setupViewOutgoing()
        }
      } else {
        setupViewActive()
      }
    })
  }

  override def onPause(): Unit = {
    super.onPause()

    stateSub.unsubscribe()
  }

  def setupViewIncoming(): Unit = {
    //Do nothing, incoming by default
    //vibrator.vibrate()
    vibrator.cancel()
  }

  def setupViewOutgoing(): Unit = {
    hideAnswerButton()
  }

  def setupViewActive(): Unit = {
    hideAnswerButton()
  }

  def hideAnswerButton(): Unit = {
    answerCallButton.setVisibility(View.GONE)
    val lp = new LinearLayout.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT, 1f)
    endCallButton.setLayoutParams(lp)
  }

  def setupViewOnHold(): Unit = {
    //NA TODO
  }

  def hideViewOnHold(): Unit = {
    //NA TODO
  }

  def endCall(): Unit = {
    finish()
  }
}

