package chat.tox.antox.fragments

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.util.Log
import android.view.View.OnClickListener
import android.view._
import android.widget.{Button, EditText, Toast}
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.toxdns.ToxDNS
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.ToxAddress
import im.tox.tox4j.core.ToxCoreConstants
import im.tox.tox4j.exceptions.ToxException
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

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreate(savedInstanceState)

    val rootView = inflater.inflate(R.layout.fragment_add_friend, container, false)
    getActivity.overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    context = getActivity.getApplicationContext

    text = getString(R.string.addfriend_friend_added)
    friendID = rootView.findViewById(R.id.addfriend_key).asInstanceOf[EditText]
    friendMessage = rootView.findViewById(R.id.addfriend_message).asInstanceOf[EditText]
    friendAlias = rootView.findViewById(R.id.addfriend_friendAlias).asInstanceOf[EditText]

    friendMessage.setFilters(Array[InputFilter](new LengthFilter(ToxCoreConstants.MAX_FRIEND_REQUEST_LENGTH)))

    rootView.findViewById(R.id.add_friend_button).asInstanceOf[Button].setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        addFriend(view)
      }
    })
    rootView
  }

  override def onPause() = {
    super.onPause()
  }

  def inputID(input: String) {
    val addFriendKey = getView.findViewById(R.id.addfriend_key).asInstanceOf[EditText]
    val friendAddress = (if (input.toLowerCase.contains("tox:")) input.substring(4) else input)
      .replaceAll("\uFEFF", "").replace(" ", "") //remove start-of-file unicode char and spaces
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
    var tmp = preferences.getString("tox_id", "")

    if (tmp.toLowerCase.startsWith("tox:")) {
      tmp = tmp.substring(4)
    }

    new ToxAddress(tmp) == address
  }

  private def checkAndSend(rawAddress: String, originalUsername: String): Int = {
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
            ToxSingleton.tox.addFriend(address, message)
            ToxSingleton.save()
          } catch {
            case e: ToxException[_] => e.printStackTrace()
          }
          Log.d("AddFriendActivity", "Adding friend to database")
          db.addFriend(key, originalUsername, alias, "Friend Request Sent")
        } else {
          toast = Toast.makeText(context, getResources.getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT)
          toast.show()
          -2
        }
        toast = Toast.makeText(context, text, duration)
        toast.show()
        0
      } else {

        toast = Toast.makeText(context, getResources.getString(R.string.addfriend_own_key), Toast.LENGTH_SHORT)
        toast.show()
        -3
      }
    } else {

      showToastInvalidID()
      -1
    }
  }

  def addFriend(view: View) {
    if (friendID.length == 76) {
      // Attempt to use ID as a Tox ID
      val result = checkAndSend(friendID.getText.toString, _originalUsername)
      if (result == 0) {
        val update = new Intent(Constants.BROADCAST_ACTION)
        update.putExtra("action", Constants.UPDATE)
        LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
        val i = new Intent()
        getActivity.setResult(Activity.RESULT_OK, i)
        getActivity.finish()
      }
    } else {
      // Attempt to use ID as a dns account name
      _originalUsername = friendID.getText.toString
      try {
        ToxDNS.lookup(_originalUsername)
          .subscribeOn(IOScheduler())
          .observeOn(AndroidMainThreadScheduler())
          .subscribe((m_key: Option[String]) => {
            m_key match {
              case Some(key) =>
                val result = checkAndSend(key, _originalUsername)
                if (result == 0) {
                  val update = new Intent(Constants.BROADCAST_ACTION)
                  update.putExtra("action", Constants.UPDATE)
                  LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
                  val i = new Intent()
                  getActivity.setResult(Activity.RESULT_OK, i)
                  getActivity.finish()
                }
              case None => showToastInvalidID()
            }
          })
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
