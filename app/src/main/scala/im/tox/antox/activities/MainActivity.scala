package im.tox.antox.activities

import java.util
import java.util.{ArrayList, Locale}

import android.app.{Activity, AlertDialog, NotificationManager}
import android.content.res.Configuration
import android.content.{Context, DialogInterface, Intent, SharedPreferences}
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBarActivity, ActionBarDrawerToggle}
import android.support.v7.widget.Toolbar
import android.view.{MenuItem, View, WindowManager}
import android.widget.{AdapterView, ListView, Toast}
import im.tox.antoxnightly.R
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.{ToxDoService, ToxSingleton}
import im.tox.antox.utils.{BitmapManager, Constants, DrawerArrayAdapter, DrawerItem}

class MainActivity extends ActionBarActivity {

  var request: View = _

  var preferences: SharedPreferences = _

  private var mDrawerLayout: DrawerLayout = _

  private var mToolbar: Toolbar = _

  private var mDrawerList: ListView = _

  private var mDrawerToggle: ActionBarDrawerToggle = _

  private def selectItem(position: Int) {
    if (position == 0) {
      val intent = new Intent(this, classOf[ProfileSettingsActivity])
      startActivity(intent)
    } else if (position == 1) {
      val intent = new Intent(this, classOf[Settings])
      startActivity(intent)
    } else if (position == 2) {
      Toast.makeText(this, "Coming soon...", Toast.LENGTH_LONG)
        .show()
    } else if (position == 3) {
      val intent = new Intent(this, classOf[About])
      startActivity(intent)
    } else if (position == 4) {
      val intent = new Intent(this, classOf[License])
      startActivity(intent)
    } else if (position == 5) {
      val intent = new Intent(this, classOf[LoginActivity])

      // Set logged out
      val preferences = PreferenceManager.getDefaultSharedPreferences(this)
      val editor = preferences.edit()
      editor.putBoolean("loggedin", false)
      editor.apply()

      // Stop Tox service
      val service = new Intent(this, classOf[ToxDoService])
      getApplicationContext.stopService(service)

      // Launch login activity and stop this one
      startActivity(intent)
      finish()
    }
    mDrawerList.setItemChecked(position, true)
    mDrawerLayout.closeDrawer(mDrawerList)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val id = item.getItemId
    if (id == android.R.id.home) {
      if (mDrawerToggle.onOptionsItemSelected(item)) {
        return true
      }
    }
    super.onOptionsItemSelected(item)
  }

  protected override def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)
    mDrawerToggle.syncState()
  }

  override def onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    mDrawerToggle.onConfigurationChanged(newConfig)
  }

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    preferences = PreferenceManager.getDefaultSharedPreferences(this)

    // The app will control the voice call audio level
    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)

    // Set the right language
    val localeStr = preferences.getString("locale", "-1")
    if (localeStr == "-1") {
      val editor = preferences.edit()
      val currentLanguage = getResources.getConfiguration.locale.getLanguage.toLowerCase
      val currentCountry = getResources.getConfiguration.locale.getCountry
      editor.putString("locale", currentLanguage + "_" + currentCountry)
      editor.apply()
    } else {
      var locale = getResources.getConfiguration.locale
      val spliteLocation = localeStr.indexOf("_")

      if (spliteLocation != -1) {
        val languageStr = localeStr.substring(0, spliteLocation)
        val countryStr = localeStr.substring(spliteLocation + 1)
        locale = new Locale(languageStr, countryStr)
      } else {
        locale = new Locale(localeStr)
      }
      Locale.setDefault(locale)
      val config = new Configuration()
      config.locale = locale
      getApplicationContext.getResources.updateConfiguration(config, getApplicationContext.getResources.getDisplayMetrics)
    }

    setContentView(R.layout.activity_main)

    mToolbar = findViewById(R.drawable.ic_navigation_drawer).asInstanceOf[Toolbar]
    mDrawerLayout = findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
    mDrawerList = findViewById(R.id.left_drawer).asInstanceOf[ListView]
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
    val list = new util.ArrayList[DrawerItem]()
    list.add(new DrawerItem(getString(R.string.n_profile_options), R.drawable.ic_profile))
    list.add(new DrawerItem(getString(R.string.n_settings), R.drawable.ic_menu_settings))
    list.add(new DrawerItem(getString(R.string.n_create_group), R.drawable.ic_social_add_group))
    list.add(new DrawerItem(getString(R.string.n_about), R.drawable.ic_menu_help))
    list.add(new DrawerItem(getString(R.string.n_open_source), R.drawable.ic_opensource))
    list.add(new DrawerItem(getString(R.string.n_logout), R.drawable.ic_logout))
    val drawerListAdapter = new DrawerArrayAdapter(this, R.layout.rowlayout_drawer, list)
    mDrawerList.setAdapter(drawerListAdapter)
    mDrawerList.setOnItemClickListener(new DrawerItemClickListener())
    if (getSupportActionBar != null) {
      getSupportActionBar.setDisplayHomeAsUpEnabled(true)
      getSupportActionBar.setHomeButtonEnabled(true)
    }
    mDrawerToggle = new ActionBarDrawerToggle(
      this, mDrawerLayout, mToolbar,
      R.string.drawer_open, R.string.drawer_close) {

      override def onDrawerClosed(view: View) {
        ActivityCompat.invalidateOptionsMenu(MainActivity.this)
      }

      override def onDrawerOpened(drawerView: View) {
        ActivityCompat.invalidateOptionsMenu(MainActivity.this)
      }
    }
    mDrawerLayout.setDrawerListener(mDrawerToggle)
    mDrawerToggle.syncState()

    // Fix for Android 4.1.x
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      getWindow.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    // Check to see if Internet is available and show a warning if it isn't
    val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connMgr.getActiveNetworkInfo
    if (networkInfo != null && !networkInfo.isConnected) {
      showAlertDialog(MainActivity.this, getString(R.string.main_no_internet), getString(R.string.main_not_connected))
    }

    ToxSingleton.mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
      new BitmapManager()

    Constants.epoch = System.currentTimeMillis() / 1000

    ToxSingleton.updateFriendsList(this)
    ToxSingleton.updateLastMessageMap(this)
    ToxSingleton.updateUnreadCountMap(this)

    val db = new AntoxDB(getApplicationContext)
    db.clearFileNumbers()
    db.close()

    getSupportActionBar.setElevation(0)

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

  protected override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == Constants.ADD_FRIEND_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      ToxSingleton.updateFriendsList(this)
    }
  }

  override def onPause() {
    super.onPause()
    ToxSingleton.chatActive = false
  }

  override def onDestroy() {
    super.onDestroy()
    State.calls.removeAll()
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

  private class DrawerItemClickListener extends AdapterView.OnItemClickListener {

    override def onItemClick(parent: AdapterView[_],
                             view: View,
                             position: Int,
                             id: Long) {
      selectItem(position)
    }
  }

}
