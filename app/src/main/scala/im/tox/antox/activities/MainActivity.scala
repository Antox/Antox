package im.tox.antox.activities

import java.util
import java.util.Locale

import android.app.{Activity, AlertDialog, NotificationManager}
import android.content.res.Configuration
import android.content.{Context, DialogInterface, Intent, SharedPreferences}
import android.media.AudioManager
import android.net.{Uri, ConnectivityManager}
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBarDrawerToggle, AppCompatActivity}
import android.support.v7.widget.Toolbar
import android.view.{Gravity, MenuItem, View, WindowManager}
import android.support.annotation.IntDef
import android.widget.{TextView, AdapterView, ListView, Toast}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.antox.callbacks.{SelfConnectionStatusChangeListener, AntoxOnSelfConnectionStatusCallback}
import im.tox.antox.data.{UserDB, State, AntoxDB}
import im.tox.antox.fragments.MainDrawerFragment
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils._
import im.tox.antox.wrapper.FileKind.AVATAR
import im.tox.antox.wrapper.UserStatus
import im.tox.antoxnightly.R
import im.tox.tox4j.core.enums.ToxConnection

class MainActivity extends AppCompatActivity {

  var request: View = _

  var preferences: SharedPreferences = _

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_main)

    preferences = PreferenceManager.getDefaultSharedPreferences(this)

    // Use a toolbar so that the drawer goes above the action bar
    val toolbar = findViewById(R.id.toolbar).asInstanceOf[Toolbar]
    setSupportActionBar(toolbar)

    getSupportActionBar.setHomeAsUpIndicator(R.drawable.ic_menu)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    // The app will control the voice call audio level
    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)

    // Set the right language
    setLanguage()

    // Fix for Android 4.1.x
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    // Check to see if Internet is potentially available and show a warning if it isn't
    if (!isNetworkConnected)
      showAlertDialog(MainActivity.this, getString(R.string.main_no_internet), getString(R.string.main_not_connected))

    // Give ToxSingleton an instance of notification manager for use in displaying notifications from callbacks
    ToxSingleton.mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

    // Initialise the bitmap manager for storing bitmaps in a cache
    new BitmapManager()

    val db = State.db
    db.clearFileNumbers()

    // Removes the drop shadow from the actionbar as it overlaps the tabs
    getSupportActionBar.setElevation(0)
  }

  def onClickAdd(v: View) {
    val intent = new Intent(this, classOf[AddActivity])
    startActivityForResult(intent, Constants.ADD_FRIEND_REQUEST_CODE)
  }

  override def onPause() {
    super.onPause()
    ToxSingleton.chatActive = false
  }

  override def onDestroy() {
    super.onDestroy()
    State.calls.removeAll()
  }

  /**
   * Displays a generic dialog using the strings passed in.
   * TODO: Should maybe be refactored into separate class and used for other dialogs?
   */
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

  /**
   * Checks to see if Wifi or Mobile have a network connection
   */
  private def isNetworkConnected: Boolean = {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connectivityManager.getAllNetworkInfo

    for (info <- networkInfo) {
      if ("WIFI".equalsIgnoreCase(info.getTypeName) && info.isConnected)
        return true

      else if ("MOBILE".equalsIgnoreCase(info.getTypeName) && info.isConnected)
        return true
    }

    false
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val id = item.getItemId
    if (id == android.R.id.home) {
      val drawer = getSupportFragmentManager.findFragmentById(R.id.drawer).asInstanceOf[MainDrawerFragment]
      drawer.openDrawer()
      return true
    }
    super.onOptionsItemSelected(item)
  }

  private def setLanguage() {
    val language = preferences.getString("language", "-1")

    if (language == "-1") {
      val editor = preferences.edit()
      val currentLanguage = getResources.getConfiguration.locale.getCountry.toLowerCase
      editor.putString("language", currentLanguage)
      editor.apply()
    } else {
      val locale = new Locale(language)
      Locale.setDefault(locale)
      val config = new Configuration()
      config.locale = locale
      getApplicationContext.getResources.updateConfiguration(config, getApplicationContext.getResources.getDisplayMetrics)
    }
  }
}
