package im.tox.antox.activities

import android.app.Activity
import android.os.{Build, Bundle}
import android.view.View.OnClickListener
import android.view.{View, WindowManager}
import android.widget.{ImageView, TextView}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.BitmapManager
import im.tox.antox.wrapper.ToxKey
import im.tox.antoxnightly.R


class CallActivity extends Activity {

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_call)

    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    /* Set up avatar and name */
    val name = findViewById(R.id.friendName).asInstanceOf[TextView]
    name.setText(getIntent.getStringExtra("name"))

    val key = getIntent.getStringExtra("key")
    if (key != null) { // Will happen when you attempt to call a user you have added but hasnt come online yet
      val friend = ToxSingleton.getAntoxFriend(new ToxKey(key))
      friend.foreach(friend => {
        friend.getAvatar.foreach(avatar => {
          val avatarView = findViewById(R.id.avatar).asInstanceOf[ImageView]
          BitmapManager.load(avatar, avatarView, isAvatar = true)
        })
      })
    }

    /* Set up the volume/mic buttons */
    val micoff = findViewById(R.id.mic_off)
    val micon = findViewById(R.id.mic_on)
    val volumeon = findViewById(R.id.volume_on)
    val volumeoff = findViewById(R.id.volume_off)

    micoff.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        micoff.setVisibility(View.GONE)
        micon.setVisibility(View.VISIBLE)

        // Mute the microphone
      }
    })

    micon.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        micoff.setVisibility(View.VISIBLE)
        micon.setVisibility(View.GONE)

        // Enable the microphone
      }
    })

    volumeon.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        volumeoff.setVisibility(View.VISIBLE)
        volumeon.setVisibility(View.GONE)

        // Mute the volume
      }
    })

    volumeoff.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        volumeoff.setVisibility(View.GONE)
        volumeon.setVisibility(View.VISIBLE)

        // Turn on the volume
      }
    })

    /* Set up the end call and av buttons */
    val endCall = findViewById(R.id.endCallCircle)
    val startVideo = findViewById(R.id.startVideoCircle)

    endCall.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        // End the call

        finish()
      }
    })

    startVideo.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        // Start video call
      }
    })
  }
}

