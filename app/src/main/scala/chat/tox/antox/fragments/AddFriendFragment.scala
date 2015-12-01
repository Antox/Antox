package chat.tox.antox.fragments

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.View.OnClickListener
import android.view._
import android.widget.{Button, EditText, Toast}
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.toxme.ToxMe
import chat.tox.antox.utils.{AntoxNotificationManager, Constants, UiUtils}
import chat.tox.antox.wrapper.ToxAddress
import im.tox.tox4j.core.ToxCoreConstants
import im.tox.tox4j.core.data.ToxFriendRequestMessage
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.Subscription
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

class AddFriendFragment extends Fragment with InputableID {

  var _friendID: String = ""

  var _friendCHECK: String = ""

  var _originalUsername: String = ""

  var context: Context = _

  var text: CharSequence = _

  var duration: Int = Toast.LENGTH_SHORT

  var toast: Toast = _

  var friendID: EditText = _

  var friendMessage: EditText = _

  var friendAlias: EditText = _

  var lookupSubscription: Option[Subscription] = None

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreate(savedInstanceState)

    val rootView = inflater.inflate(R.layout.fragment_add_friend, container, false)
    getActivity.overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    context = getActivity.getApplicationContext

    text = getString(R.string.addfriend_friend_added)
    friendID = rootView.findViewById(R.id.addfriend_key).asInstanceOf[EditText]
    friendMessage = rootView.findViewById(R.id.addfriend_message).asInstanceOf[EditText]
    friendAlias = rootView.findViewById(R.id.addfriend_friendAlias).asInstanceOf[EditText]

    friendMessage.setFilters(Array[InputFilter](new LengthFilter(ToxCoreConstants.MaxFriendRequestLength)))

    rootView.findViewById(R.id.add_friend_button).asInstanceOf[Button].setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        addFriend(view)
      }
    })
    rootView
  }

  override def onPause(): Unit = {
    super.onPause()
    lookupSubscription.foreach(_.unsubscribe())
  }

  def inputID(input: String) {
    val addFriendKey = getView.findViewById(R.id.addfriend_key).asInstanceOf[EditText]
    val friendAddress = UiUtils.sanitizeAddress(ToxAddress.removePrefix(input))

    if (ToxAddress.isAddressValid(friendAddress)) {
      addFriendKey.setText(friendAddress)
    } else {
      val context = getActivity.getApplicationContext
      val toast = Toast.makeText(context, getResources.getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT)
      toast.show()
    }
  }

  private def isAddressOwn(address: ToxAddress): Boolean = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val ownAddress = ToxAddress.removePrefix(preferences.getString("tox_id", ""))

    new ToxAddress(ownAddress) == address
  }

  private def checkAndSend(rawAddress: String, originalUsername: String): Boolean = {
    if (ToxAddress.isAddressValid(rawAddress)) {
      val address = new ToxAddress(rawAddress)

      if (!isAddressOwn(address)) {
        val key = address.key
        var message = friendMessage.getText.toString
        val alias = friendAlias.getText.toString

        if (message == "") message = getString(R.string.addfriend_default_message)

        val db = State.db
        if (!db.doesContactExist(key)) {
          try {
            ToxSingleton.tox.addFriend(address, ToxFriendRequestMessage.unsafeFromValue(message.getBytes))
            ToxSingleton.save()
          } catch {
            case e: ToxException[_] => e.printStackTrace()
          }
          db.addFriend(key, originalUsername, alias, "Friend Request Sent")

          //prevent already-added friend from having an existing friend request
          db.deleteFriendRequest(key)
          AntoxNotificationManager.clearRequestNotification(key)
        } else {
          toast = Toast.makeText(context, getResources.getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT)
          toast.show()
        }
        toast = Toast.makeText(context, text, duration)
        toast.show()
        true
      } else {

        toast = Toast.makeText(context, getResources.getString(R.string.addfriend_own_key), Toast.LENGTH_SHORT)
        toast.show()
        false
      }
    } else {
      showToastInvalidID()
      false
    }
  }

  def addFriend(view: View) {
    if (friendID.length == 76) {
      // Attempt to use ID as a Tox ID
      val result = checkAndSend(friendID.getText.toString, _originalUsername)
      if (result) {
        val update = new Intent(Constants.BROADCAST_ACTION)
        update.putExtra("action", Constants.UPDATE)
        LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
        val i = new Intent()
        getActivity.setResult(Activity.RESULT_OK, i)
        getActivity.finish()
      }
    } else {
      // Attempt to use ID as a toxme account name
      _originalUsername = friendID.getText.toString
      try {
        lookupSubscription = Some(
          ToxMe.lookup(_originalUsername)
          .subscribeOn(IOScheduler())
          .observeOn(AndroidMainThreadScheduler())
          .subscribe((m_key: Option[String]) => {
            m_key match {
              case Some(key) =>
                val result = checkAndSend(key, _originalUsername)
                if (result) {
                  val update = new Intent(Constants.BROADCAST_ACTION)
                  update.putExtra("action", Constants.UPDATE)
                  LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
                  val i = new Intent()
                  getActivity.setResult(Activity.RESULT_OK, i)
                  getActivity.finish()
                }
              case None => showToastInvalidID()
            }
          }))
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

  def showToastInvalidID(): Unit = {
    toast = Toast.makeText(context, getResources.getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT)
    toast.show()
  }
}
