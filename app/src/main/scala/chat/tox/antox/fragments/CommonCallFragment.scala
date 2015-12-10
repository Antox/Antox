package chat.tox.antox.fragments

import android.content.Context
import android.media.MediaPlayer.OnCompletionListener
import android.media.{AudioManager, MediaPlayer}
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.TextView
import chat.tox.antox.R
import chat.tox.antox.av.Call
import chat.tox.antox.data.State
import chat.tox.antox.utils.{BitmapManager, MediaUtils}
import chat.tox.antox.wrapper.{CallNumber, ContactKey, FriendInfo, FriendKey}
import de.hdodenhof.circleimageview.CircleImageView
import rx.lang.scala.subscriptions.CompositeSubscription

object CommonCallFragment {
  val EXTRA_CALL_NUMBER = "call_number"
  val EXTRA_ACTIVE_KEY = "active_key"
  val EXTRA_FRAGMENT_LAYOUT = "fragment_layout"
}

abstract class CommonCallFragment extends Fragment {

  var call: Call = _
  var activeKey: ContactKey = _
  var callLayout: Int = _

  var callStateView: TextView = _
  var nameView: TextView = _
  var avatarView: CircleImageView = _

  var endCallButton: View = _

  var callEndedSound: MediaPlayer = _

  val compositeSubscription = CompositeSubscription()

  var audioManager: AudioManager = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    call =
      State.callManager.get(CallNumber(getArguments.getInt(CommonCallFragment.EXTRA_CALL_NUMBER)))
        .getOrElse(throw new IllegalStateException("Call fragment extras must be valid."))
    activeKey = FriendKey(getArguments.getString(CommonCallFragment.EXTRA_ACTIVE_KEY))
    callLayout = getArguments.getInt(CommonCallFragment.EXTRA_FRAGMENT_LAYOUT)

    audioManager = getActivity.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]

    callEndedSound = MediaUtils.setupSound(getActivity, R.raw.end_call, AudioManager.STREAM_VOICE_CALL, looping = false)
  }

  private def updateDisplayedState(friendInfoList: Seq[FriendInfo]): Unit = {
    val mFriend: Option[FriendInfo] = friendInfoList.find(f => f.key == activeKey)

    mFriend match {
      case Some(friend) =>
        nameView.setText(friend.getDisplayName)

        val avatar = friend.avatar
        avatar.foreach(avatar => {
          val bitmap = BitmapManager.loadBlocking(avatar, isAvatar = true)
          avatarView.setImageBitmap(bitmap)
        })

      case None =>
        nameView.setText("")
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    val rootView = inflater.inflate(callLayout, container, false)

    callStateView = rootView.findViewById(R.id.call_state_text).asInstanceOf[TextView]

    avatarView = rootView.findViewById(R.id.call_avatar).asInstanceOf[CircleImageView]
    nameView = rootView.findViewById(R.id.friend_name).asInstanceOf[TextView]

    /* Set up the end call and av buttons */
    endCallButton = rootView.findViewById(R.id.end_call_circle)

    endCallButton.setOnClickListener(new OnClickListener {
      override def onClick(view: View) = {
        call.end()
        endCallButton.setEnabled(false) //don't let the user press end call more than once
      }
    })

    compositeSubscription +=
      call.callEndedObservable.subscribe(_ => {
        onCallEnded()
      })

    // update displayed friend info on change
    compositeSubscription +=
      State.db.friendInfoList
        .subscribe(fi => {
          updateDisplayedState(fi)
        })

    rootView
  }

  // Called when the call ends (both by the user and by friend)
  def onCallEnded(): Unit = {
    getActivity.finish()

    callEndedSound.start()
    callEndedSound.setOnCompletionListener(new OnCompletionListener {
      override def onCompletion(mediaPlayer: MediaPlayer): Unit = {
        callEndedSound.release()
      }
    })
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    compositeSubscription.unsubscribe()
  }
}
