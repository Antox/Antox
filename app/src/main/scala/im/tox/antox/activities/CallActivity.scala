package im.tox.antox.activities

import android.app.Activity
import android.os.{Build, Bundle}
import android.view.View.OnClickListener
import android.view.{View, WindowManager}
import android.widget.{ImageView, TextView}
import im.tox.antox.av.Call
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.BitmapManager
import im.tox.antox.wrapper.ToxKey
import im.tox.antoxnightly.R


class CallActivity extends Activity {

  var call: Call = _

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_call)

    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
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
      override def onClick(view: View) = {
        micOff.setVisibility(View.GONE)
        micOn.setVisibility(View.VISIBLE)

        call.muteSelfAudio()
      }
    })

    micOn.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        micOff.setVisibility(View.VISIBLE)
        micOn.setVisibility(View.GONE)

        call.unmuteSelfAudio()
      }
    })

    volumeOn.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        volumeOff.setVisibility(View.VISIBLE)
        volumeOn.setVisibility(View.GONE)

        call.muteSpeaker()
      }
    })

    volumeOff.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        volumeOff.setVisibility(View.GONE)
        volumeOn.setVisibility(View.VISIBLE)

        call.unmuteSpeaker()
      }
    })

    videoOn.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        videoOff.setVisibility(View.GONE)
        videoOn.setVisibility(View.VISIBLE)

        call.showSelfVideo()
      }
    })


    videoOff.setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        videoOn.setVisibility(View.GONE)
        videoOff.setVisibility(View.VISIBLE)

        call.hideSelfVideo()
      }
    })

    /* Set up the end call and av buttons */
    val endCall = findViewById(R.id.endCallCircle)

    endCall.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.end()

        finish()
      }
    })

  }
}

