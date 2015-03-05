package im.tox.antox.activities

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.v4.app.{Fragment, NavUtils}
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.ActionBarActivity
import android.util.Log
import android.view._
import android.widget.{EditText, Toast}
import im.tox.QR.IntentIntegrator
import im.tox.antoxnightly.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{Constants, Hex}
import im.tox.tox4j.exceptions.ToxException
import org.xbill.DNS.{Lookup, TXTRecord, Type}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
//remove if not needed

class AddFriendFragment extends Fragment {

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

  override def onCreate(savedInstanceState: Bundle): Unit = {

  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    val rootView = inflater.inflate(R.layout.fragment_add_friend, container, false);
    getActivity.overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    context = getActivity.getApplicationContext

    text = getString(R.string.addfriend_friend_added)
    friendID = rootView.findViewById(R.id.addfriend_key).asInstanceOf[EditText]
    friendMessage = rootView.findViewById(R.id.addfriend_message).asInstanceOf[EditText]
    friendAlias = rootView.findViewById(R.id.addfriend_friendAlias).asInstanceOf[EditText]

    val intent = getActivity.getIntent
    if (Intent.ACTION_VIEW == intent.getAction && intent != null) {
      // Handle incoming tox uri links
      val friendID = rootView.findViewById(R.id.addfriend_key).asInstanceOf[EditText]
      var uri: Uri = null
      uri = intent.getData
      if (uri != null) friendID.setText(uri.getHost)
    }

    rootView
  }

  override def onPause() = {
    super.onPause()
  }

  private def isAddressOwn(key: String): Boolean = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    var tmp = preferences.getString("tox_id", "")

    if (tmp.toLowerCase.startsWith("tox:"))
      tmp = tmp.substring(4)

    if (tmp == key)
      true
    else
      false
  }

  private def checkAndSend(address: String, originalUsername: String): Int = {
    if (!isAddressOwn(address)) {
      if (validateFriendKey(address)) {
        val key = ToxSingleton.keyFromAddress(address)
        var message = friendMessage.getText.toString
        val alias = friendAlias.getText.toString
        if (message == "") message = getString(R.string.addfriend_default_message)
        val friendData = Array(message, alias)
        val db = new AntoxDB(getActivity.getApplicationContext)
        if (!db.doesFriendExist(key)) {
          try {
            ToxSingleton.tox.addFriend(address, friendData(0))
            ToxSingleton.save()
          } catch {
            case e: ToxException => e.printStackTrace()
          }
          Log.d("AddFriendActivity", "Adding friend to database")
          db.addFriend(key, "Friend Request Sent", alias, originalUsername)
        } else {
          db.close()
          toast = Toast.makeText(context, getResources.getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT)
          toast.show()
          -2
        }
        db.close()
        toast = Toast.makeText(context, text, duration)
        toast.show()
        0
      } else {
        toast = Toast.makeText(context, getResources.getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT)
        toast.show()
        -1
      }
    } else {
      toast = Toast.makeText(context, getResources.getString(R.string.addfriend_own_key), Toast.LENGTH_SHORT)
      toast.show()
      -3
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
        DNSLookup(_originalUsername)
          .subscribeOn(IOScheduler())
          .observeOn(AndroidMainThreadScheduler())
          .subscribe((tup: (String, Option[String])) => {
            tup match {
              case (key, mCheck) => {
                mCheck match {
                  case None => {
                    val result = checkAndSend(key, _originalUsername)
                    if (result == 0) {
                      val update = new Intent(Constants.BROADCAST_ACTION)
                      update.putExtra("action", Constants.UPDATE)
                      LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
                      val i = new Intent()
                      getActivity.setResult(Activity.RESULT_OK, i)
                      getActivity.finish()
                    }
                  }
                  case Some(_) => throw new Exception("this shouldn't happen")
                }
              }
            }
          })
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

  def onQRScanResult(scanResult: String) {
    val addFriendKey = getView.findViewById(R.id.addfriend_key).asInstanceOf[EditText]
    val friendKey = (if (scanResult.toLowerCase.contains("tox:")) scanResult.substring(4) else scanResult)
                  .replaceAll("\uFEFF", "").replace(" ", "") //remove start-of-file unicode char and spaces
    if (validateFriendKey(friendKey)) {
      addFriendKey.setText(scanResult)
    } else {
      val context = getActivity.getApplicationContext
      val toast = Toast.makeText(context, getResources.getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT)
      toast.show()
    }
  }

  private def validateFriendKey(friendKey: String): Boolean = {
    if (friendKey.length != 76 || friendKey.matches("[[:xdigit:]]")) {
      return false
    }
    var x = 0
    try {
      var i = 0
      while (i < friendKey.length) {
        x = x ^
          java.lang.Integer.valueOf(friendKey.substring(i, i + 4), 16)
        i += 4
      }
    } catch {
      case e: NumberFormatException => return false
    }
    x == 0
  }

  private def DNSLookup(input: String): Observable[(String, Option[String])] = {
    Observable(subscriber => {
      var user: String = null
      var domain: String = null
      var lookup: String = null
      if (!input.contains("@")) {
        user = input
        domain = "toxme.se"
        lookup = user + "._tox." + domain
      } else {
        user = input.substring(0, input.indexOf("@"))
        domain = input.substring(input.indexOf("@") + 1)
        lookup = user + "._tox." + domain
      }
      var txt: TXTRecord = null
      try {
        val records = new Lookup(lookup, Type.TXT).run()
        txt = records(0).asInstanceOf[TXTRecord]
        val txtString = txt.toString.substring(txt.toString.indexOf('"'))
        if (txtString.contains("tox1")) {
          val key = txtString.substring(11, 11 + 76)
          subscriber.onNext((key, None))
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
      subscriber.onCompleted()
    })
  }
}
