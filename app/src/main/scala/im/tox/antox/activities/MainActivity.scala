package im.tox.antox.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarActivity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import java.util.Locale
import im.tox.antox.R
import im.tox.antox.data.AntoxDB
import im.tox.antox.fragments.DialogToxID
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.BitmapManager
import im.tox.antox.utils.Constants
import im.tox.antox.utils.Triple
import im.tox.antox.TestScala
import im.tox.jtoxcore.ToxCallType
import im.tox.jtoxcore.ToxCodecSettings
import im.tox.jtoxcore.ToxException
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
//remove if not needed
import scala.collection.JavaConversions._

class MainActivity extends ActionBarActivity with DialogToxID.DialogToxIDListener {

  var request: View = _

  var activeKeySub: Subscription = _

  var chatActiveSub: Subscription = _

  var preferences: SharedPreferences = _

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)
    preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val test = new TestScala()
    test.test()
    val language = preferences.getString("language", "-1")
    if (language == "-1") {
      val editor = preferences.edit()
      val currentLanguage = getResources.getConfiguration.locale.getCountry.toLowerCase()
      editor.putString("language", currentLanguage)
      editor.commit()
    } else {
      val locale = new Locale(language)
      Locale.setDefault(locale)
      val config = new Configuration()
      config.locale = locale
      getApplicationContext.getResources.updateConfiguration(config, getApplicationContext.getResources.getDisplayMetrics)
    }
    setContentView(R.layout.activity_main)
    getSupportActionBar.hide()
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && 
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }
    val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connMgr.getActiveNetworkInfo
    if (networkInfo != null && !networkInfo.isConnected) {
      showAlertDialog(MainActivity.this, getString(R.string.main_no_internet), getString(R.string.main_not_connected))
    }
    ToxSingleton.mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) new BitmapManager()
    Constants.epoch = System.currentTimeMillis() / 1000
    ToxSingleton.initSubjects(this)
    ToxSingleton.updateFriendsList(this)
    ToxSingleton.updateLastMessageMap(this)
    ToxSingleton.updateUnreadCountMap(this)
    val db = new AntoxDB(getApplicationContext)
    db.clearFileNumbers()
    db.close()
    updateLeftPane()
  }

  def updateLeftPane() {
    ToxSingleton.updateFriendRequests(getApplicationContext)
    ToxSingleton.updateFriendsList(getApplicationContext)
    ToxSingleton.updateMessages(getApplicationContext)
  }

  def onClickAddFriend(v: View) {
    val intent = new Intent(this, classOf[AddFriendActivity])
    startActivityForResult(intent, Constants.ADD_FRIEND_REQUEST_CODE)
  }

  def onClickVoiceCallFriend(v: View) {
    val toxCodecSettings = new ToxCodecSettings(ToxCallType.TYPE_AUDIO, 0, 0, 0, 64000, 20, 48000, 1)
    val friend = ToxSingleton.getAntoxFriend(ToxSingleton.activeKey)
    val userID = friend.getFriendnumber
    try {
      ToxSingleton.jTox.avCall(userID, toxCodecSettings, 10)
    } catch {
      case e: ToxException => 
    }
  }

  def onClickVideoCallFriend(v: View) {
  }

  protected override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == Constants.ADD_FRIEND_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      ToxSingleton.updateFriendsList(this)
    }
  }

  override def onDestroy() {
    super.onDestroy()
  }

  override def onResume() {
    super.onResume()
    preferences = PreferenceManager.getDefaultSharedPreferences(this)
    //activeKeySub = ToxSingleton
    //  .rightPaneActiveAndKeyAndIsFriendSubject
    //  //.observeOn(AndroidSchedulers.mainThread())
    //  .subscribe(new Action1[Triple[Boolean, String, Boolean]]() {

    //  override def call(rightPaneActiveAndActiveKeyAndIfFriend: Triple[Boolean, String, Boolean]) {
    //    var rightPaneActive = rightPaneActiveAndActiveKeyAndIfFriend.x
    //    var activeKey = rightPaneActiveAndActiveKeyAndIfFriend.y
    //    var isFriend = rightPaneActiveAndActiveKeyAndIfFriend.z
    //    Log.d("activeKeySub", "oldkey: " + ToxSingleton.activeKey + " newkey: " + activeKey + 
    //      " isfriend: " + 
    //      isFriend)
    //    if (activeKey == "") {
    //    } else {
    //      if (activeKey != ToxSingleton.activeKey) {
    //        ToxSingleton.doClosePaneSubject.onNext(true)
    //        if (isFriend) {
    //        } else {
    //        }
    //      }
    //    }
    //    ToxSingleton.activeKey = activeKey
    //    if (activeKey != "" && rightPaneActive && isFriend) {
    //      var antoxDB = new AntoxDB(getApplicationContext)
    //      antoxDB.markIncomingMessagesRead(activeKey)
    //      ToxSingleton.clearUselessNotifications(activeKey)
    //      ToxSingleton.updateMessages(getApplicationContext)
    //      antoxDB.close()
    //      ToxSingleton.chatActive = true
    //    } else {
    //      ToxSingleton.chatActive = false
    //    }
    //  }
    //})
  }

  override def onPause() {
    super.onPause()
    preferences = PreferenceManager.getDefaultSharedPreferences(this)
    if (preferences.getBoolean("beenLoaded", false)) {
      //activeKeySub.unsubscribe()
      ToxSingleton.chatActive = false
    }
  }

  def showAlertDialog(context: Context, title: String, message: String) {
    val alertDialog = new AlertDialog.Builder(context).create()
    alertDialog.setTitle(title)
    alertDialog.setMessage(message)
    alertDialog.setIcon(R.drawable.ic_launcher)
    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, which: Int) {
      }
    })
    alertDialog.show()
  }

  override def onDialogClick(fragment: DialogFragment) {
  }

  def copyToxID(view: View) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    val context = getApplicationContext
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[android.text.ClipboardManager]
    clipboard.setText(sharedPreferences.getString("tox_id", ""))
  }
}
