package im.tox.antox.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.support.v4.app.NavUtils
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.ActionBarActivity
import android.app.Activity
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import org.xbill.DNS.Lookup
import org.xbill.DNS.Record
import org.xbill.DNS.TXTRecord
import org.xbill.DNS.Type
import im.tox.QR.IntentIntegrator
import im.tox.QR.IntentResult
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.fragments.PinDialogFragment
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.Constants
import im.tox.jtoxcore.FriendExistsException
import im.tox.jtoxcore.ToxException
//remove if not needed
import scala.collection.JavaConversions._

class AddFriendActivity extends ActionBarActivity with PinDialogFragment.PinDialogListener {

  var _friendID: String = ""

  var _friendCHECK: String = ""

  var _originalUsername: String = ""

  var isV2: Boolean = false

  var context: Context = _

  var text: CharSequence = _

  var duration: Int = Toast.LENGTH_SHORT

  var toast: Toast = _

  var friendID: EditText = _

  var friendMessage: EditText = _

  var friendAlias: EditText = _

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && 
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }
    setContentView(R.layout.activity_add_friend)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      getSupportActionBar.setIcon(R.drawable.ic_actionbar)
    }
    context = getApplicationContext
    text = getString(R.string.addfriend_friend_added)
    friendID = findViewById(R.id.addfriend_key).asInstanceOf[EditText]
    friendMessage = findViewById(R.id.addfriend_message).asInstanceOf[EditText]
    friendAlias = findViewById(R.id.addfriend_friendAlias).asInstanceOf[EditText]
    val intent = getIntent
    if (Intent.ACTION_VIEW == intent.getAction && intent != null) {
      val friendID = findViewById(R.id.addfriend_key).asInstanceOf[EditText]
      var uri: Uri = null
      uri = intent.getData
      if (uri != null) friendID.setText(uri.getHost)
    } else if (intent.getAction == "toxv2") {
      friendID.setText(intent.getStringExtra("originalUsername"))
      friendAlias.setText(intent.getStringExtra("alias"))
      friendMessage.setText(intent.getStringExtra("message"))
      if (checkAndSend(intent.getStringExtra("key"), intent.getStringExtra("originalUsername")) == 
        0) {
        toast = Toast.makeText(context, text, duration)
        toast.show()
      } else if (checkAndSend(intent.getStringExtra("key"), intent.getStringExtra("originalUsername")) == 
        -1) {
        toast = Toast.makeText(context, getResources.getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT)
        toast.show()
        return
      } else if (checkAndSend(intent.getStringExtra("key"), intent.getStringExtra("originalUsername")) == 
        -2) {
        toast = Toast.makeText(context, getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT)
        toast.show()
      }
      val update = new Intent(Constants.BROADCAST_ACTION)
      update.putExtra("action", Constants.UPDATE)
      LocalBroadcastManager.getInstance(this).sendBroadcast(update)
      val i = new Intent()
      setResult(Activity.RESULT_OK, i)
      finish()
    }
  }

  override def onPause() = {
    super.onPause()
    if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_bottom);
  }

  private def isKeyOwn(key: String): Boolean = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    var tmp = preferences.getString("tox_id", "")
    if (tmp.toLowerCase().startsWith("tox:")) tmp = tmp.substring(4)
    if (tmp == key) true else false
  }

  private def checkAndSend(key: String, originalUsername: String): Int = {
    if (!isKeyOwn(key)) {
      if (validateFriendKey(key)) {
        val ID = key
        var message = friendMessage.getText.toString
        val alias = friendAlias.getText.toString
        if (message == "") message = getString(R.string.addfriend_default_message)
        val friendData = Array(ID, message, alias)
        val db = new AntoxDB(getApplicationContext)
        if (!db.doesFriendExist(ID)) {
          try {
            ToxSingleton.jTox.addFriend(friendData(0), friendData(1))
          } catch {
            case e: ToxException => e.printStackTrace()
            case e: FriendExistsException => e.printStackTrace()
          }
          Log.d("AddFriendActivity", "Adding friend to database")
          db.addFriend(ID, "Friend Request Sent", alias, originalUsername)
        } else {
          return -2
        }
        db.close()
        0
      } else {
        -1
      }
    } else {
      -3
    }
  }

  private def scanIntent() {
    val integrator = new IntentIntegrator(this)
    integrator.initiateScan()
  }

  def addFriend(view: View) {
    if (friendID.getText.toString.contains("@") || friendID.getText.length != 76) {
      _originalUsername = friendID.getText.toString
      try {
        new DNSLookup().execute(friendID.getText.toString).get
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    if (isV2) {
      val dialog = new PinDialogFragment()
      val bundle = new Bundle()
      bundle.putString(getResources.getString(R.string.addfriend_friend_pin_title), getResources.getString(R.string.addfriend_friend_pin_text))
      dialog.setArguments(bundle)
      dialog.show(getSupportFragmentManager, "NoticeDialogFragment")
    }
    var finalFriendKey = friendID.getText.toString
    if (_friendID != "") finalFriendKey = _friendID
    if (!isV2) {
      val result = checkAndSend(finalFriendKey, _originalUsername)
      if (result == 0) {
        toast = Toast.makeText(context, text, duration)
        toast.show()
      } else if (result == -1) {
        toast = Toast.makeText(context, getResources.getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT)
        toast.show()
        return
      } else if (result == -2) {
        toast = Toast.makeText(context, getResources.getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT)
        toast.show()
      } else if (result == -3) {
        toast = Toast.makeText(context, getResources.getString(R.string.addfriend_own_key), Toast.LENGTH_SHORT)
        toast.show()
      }
      val update = new Intent(Constants.BROADCAST_ACTION)
      update.putExtra("action", Constants.UPDATE)
      LocalBroadcastManager.getInstance(this).sendBroadcast(update)
      val i = new Intent()
      setResult(Activity.RESULT_OK, i)
      finish()
    }
  }

  override def onDialogPositiveClick(dialog: DialogFragment, pin: String) {
    val newpin = pin + "=="
    try {
      val decoded = Base64.decode(newpin, Base64.DEFAULT)
      val sb = new StringBuilder()
      for (b <- decoded) sb.append("%02x".format(b & 0xff))
      val encodedString = sb.toString
      _friendID = _friendID + encodedString + _friendCHECK
      val restart = new Intent(this, classOf[AddFriendActivity])
      restart.putExtra("key", _friendID)
      restart.putExtra("alias", friendAlias.getText.toString)
      restart.putExtra("message", friendMessage.getText.toString)
      restart.putExtra("originalUsername", _originalUsername)
      restart.setAction("toxv2")
      startActivity(restart)
      finish()
    } catch {
      case e: IllegalArgumentException => {
        val context = getApplicationContext
        val text = getString(R.string.addfriend_invalid_pin)
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, text, duration)
        toast.show()
        e.printStackTrace()
      }
    }
  }

  override def onDialogNegativeClick(dialog: DialogFragment) {
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
    val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
    if (scanResult != null) {
      if (scanResult.getContents != null) {
        val addFriendKey = findViewById(R.id.addfriend_key).asInstanceOf[EditText]
        val friendKey = (if (scanResult.getContents.toLowerCase().contains("tox:")) scanResult.getContents.substring(4) else scanResult.getContents)
        if (validateFriendKey(friendKey)) {
          addFriendKey.setText(friendKey)
        } else {
          val context = getApplicationContext
          val toast = Toast.makeText(context, getResources.getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT)
          toast.show()
        }
      }
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

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.add_friend, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
        case android.R.id.home => 
        NavUtils.navigateUpFromSameTask(this)
        true

        case R.id.scanFriend => scanIntent()
    }
    return super.onOptionsItemSelected(item)
  }

  private class DNSLookup extends AsyncTask[String, Void, Void] {

    protected def doInBackground(params: String*): Void = {
      var user: String = null
      var domain: String = null
      var lookup: String = null
      if (!params(0).contains("@")) {
        user = params(0)
        domain = "toxme.se"
        lookup = user + "._tox." + domain
      } else {
        user = params(0).substring(0, params(0).indexOf("@"))
        domain = params(0).substring(params(0).indexOf("@") + 1)
        lookup = user + "._tox." + domain
      }
      var txt: TXTRecord = null
      try {
        val records = new Lookup(lookup, Type.TXT).run()
        txt = records(0).asInstanceOf[TXTRecord]
      } catch {
        case e: Exception => e.printStackTrace()
      }
      if (txt != null) {
        val txtString = txt.toString.substring(txt.toString.indexOf('"'))
        if (txtString.contains("tox1")) {
          val key = txtString.substring(11, 11 + 76)
          _friendID = key
        } else if (txtString.contains("tox2")) {
          isV2 = true
          val key = txtString.substring(12, 12 + 64)
          val check = txtString.substring(12 + 64 + 7, 12 + 64 + 7 + 4)
          _friendID = key
          _friendCHECK = check
        }
      }
      null
    }
  }
}
