package chat.tox.antox.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.TextView
import chat.tox.antox.R
import chat.tox.antox.av.{Call, OngoingCallNotification}
import chat.tox.antox.data.State
import chat.tox.antox.utils.BitmapManager
import chat.tox.antox.wrapper.{ContactKey, FriendInfo}
import de.hdodenhof.circleimageview.CircleImageView
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.subscriptions.CompositeSubscription

abstract class CommonCallFragment(call: Call, activeKey: ContactKey, callLayout: Int) extends Fragment {

  var callStateView: TextView = _
  var nameView: TextView = _
  var avatarView: CircleImageView = _

  var endCallButton: View = _

  var maybeCallNotification: Option[OngoingCallNotification] = None

  var maybeRingtone: Option[MediaPlayer] = None

  val compositeSubscription = CompositeSubscription()

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
  }

  private def updateDisplayedState(fi: Seq[FriendInfo]): Unit = {
    val mFriend: Option[FriendInfo] = fi.find(f => f.key == activeKey)

    mFriend match {
      case Some(friend) =>
        nameView.setText(friend.getDisplayName)

        val avatar = friend.avatar
        avatar.foreach(avatar => {
          BitmapManager.load(avatar, isAvatar = true).foreach(avatarView.setImageBitmap)
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

        onCallEnded()
      }
    })

    compositeSubscription +=
      call.friendStateSubject
        .subscribe(callState => {
          if (!call.active) {
            onCallEnded()
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

    rootView
  }

  // Called when the call ends (both by the user and by friend)
  def onCallEnded(): Unit = {
    getActivity.finish()
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    compositeSubscription.unsubscribe()
    maybeCallNotification.foreach(_.cancel())
  }
}
