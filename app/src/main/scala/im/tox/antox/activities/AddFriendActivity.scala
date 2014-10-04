package im.tox.antox.activities

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.v4.app.NavUtils
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.ActionBarActivity
import android.util.Log
import android.view.{Menu, MenuItem, View, WindowManager}
import android.widget.{EditText, Toast}
import im.tox.QR.IntentIntegrator
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.Constants
import im.tox.jtoxcore.{FriendExistsException, ToxException}
import org.xbill.DNS.{Lookup, TXTRecord, Type}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
//remove if not needed

class AddFriendActivity extends ActionBarActivity {

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
      // Handle incoming tox uri links
      val friendID = findViewById(R.id.addfriend_key).asInstanceOf[EditText]
      var uri: Uri = null
      uri = intent.getData
      if (uri != null) friendID.setText(uri.getHost)
    }
  }

  override def onPause() = {
    super.onPause()
    if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_bottom);
  }

  private def isKeyOwn(key: String): Boolean = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    var tmp = preferences.getString("tox_id", "")

    if (tmp.toLowerCase().startsWith("tox:"))
      tmp = tmp.substring(4)

    if (tmp == key)
      true
    else
      false
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

  private def scanIntent() {
    val integrator = new IntentIntegrator(this)
    integrator.initiateScan()
  }

  def addFriend(view: View) {
    if (friendID.length == 76) {
      // Attempt to use ID as a Tox ID
      val result = checkAndSend(friendID.getText.toString, _originalUsername)
      if (result == 0) {
        val update = new Intent(Constants.BROADCAST_ACTION)
        update.putExtra("action", Constants.UPDATE)
        LocalBroadcastManager.getInstance(this).sendBroadcast(update)
        val i = new Intent()
        setResult(Activity.RESULT_OK, i)
        finish()
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
                      LocalBroadcastManager.getInstance(this).sendBroadcast(update)
                      val i = new Intent()
                      setResult(Activity.RESULT_OK, i)
                      finish()
                    }
                  }
                }
              }
            }
          })
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
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
