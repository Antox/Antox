package im.tox.antox.activities

import java.util
import java.util.Locale

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
import android.support.v7.app.{ActionBarDrawerToggle, AppCompatActivity}
import android.support.v7.widget.Toolbar
import android.view.{MenuItem, View, WindowManager}
import android.widget.{AdapterView, ListView, Toast}
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils._
import im.tox.antoxnightly.R

class MainActivity extends AppCompatActivity {

  var request: View = _

  var preferences: SharedPreferences = _

  private var mDrawerLayout: DrawerLayout = _

  private var mToolbar: Toolbar = _

  private var mDrawerList: ListView = _

  private var mDrawerToggle: ActionBarDrawerToggle = _

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_main)

    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    getSupportActionBar.setHomeButtonEnabled(true)

    // The app will control the voice call audio level
    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)

    // Set the right language
    setLanguage()

    // Set up the navigation drawer
    mToolbar = findViewById(R.drawable.ic_navigation_drawer).asInstanceOf[Toolbar]
    mDrawerLayout = findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
    mDrawerList = findViewById(R.id.left_drawer).asInstanceOf[ListView]
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
    val list = new util.ArrayList[DrawerItem]()
    list.add(new DrawerItem(getString(R.string.n_profile_options), R.drawable.ic_person_add_white_24dp))
    list.add(new DrawerItem(getString(R.string.n_settings), R.drawable.ic_settings_white_24dp))
    list.add(new DrawerItem(getString(R.string.n_create_group), R.drawable.ic_group_add_white_24dp))
    list.add(new DrawerItem(getString(R.string.n_about), R.drawable.ic_info_outline_white_24dp))
    list.add(new DrawerItem(getString(R.string.n_logout), R.drawable.ic_arrow_back_white_24dp))
    val drawerListAdapter = new DrawerArrayAdapter(this, R.layout.rowlayout_drawer, list)
    mDrawerList.setAdapter(drawerListAdapter)
    mDrawerList.setOnItemClickListener(new DrawerItemClickListener())

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

    // Check to see if Internet is potentially available and show a warning if it isn't
    if (!isNetworkConnected)
      showAlertDialog(MainActivity.this, getString(R.string.main_no_internet), getString(R.string.main_not_connected))

    // Give ToxSingleton an instance of notification manager for use in displaying notifications from callbacks
    ToxSingleton.mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

    // Initialise the bitmap manager for storing bitmaps in a cache
    new BitmapManager()

    val db = new AntoxDB(getApplicationContext)
    db.clearFileNumbers()
    db.close()

    // Removes the drop shadow from the actionbar as it overlaps the tabs
    getSupportActionBar.setElevation(0)

    ToxSingleton.updateLastMessageMap(this)
    ToxSingleton.updateUnreadCountMap(this)
    updateLeftPane()
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

  def updateLeftPane() {
    ToxSingleton.updateFriendRequests(getApplicationContext)
    ToxSingleton.updateFriendsList(getApplicationContext)
    ToxSingleton.updateMessages(getApplicationContext)
    ToxSingleton.updateGroupInvites(getApplicationContext)
    ToxSingleton.updateGroupList(getApplicationContext)
  }

  def onClickAdd(v: View) {
    val intent = new Intent(this, classOf[AddActivity])
    startActivityForResult(intent, Constants.ADD_FRIEND_REQUEST_CODE)
  }

  protected override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == Constants.ADD_FRIEND_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      ToxSingleton.updateFriendsList(this)
      ToxSingleton.updateGroupList(this)
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

  /**
   * Displays a generic dialog using the strings passed in.
   * Should maybe be refactored into separate class and used for other dialogs?
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

  private class DrawerItemClickListener extends AdapterView.OnItemClickListener {

    override def onItemClick(parent: AdapterView[_],
                             view: View,
                             position: Int,
                             id: Long) {
      selectItem(position)
    }
  }

  /**
   * This method is called by the DrawerItemClickListener above and starts a new activity
   * based on the item selected
   */
  private def selectItem(position: Int) {
    if (position == 0) {
      val intent = new Intent(this, classOf[ProfileSettingsActivity])
      startActivity(intent)
    } else if (position == 1) {
      val intent = new Intent(this, classOf[SettingsActivity])
      startActivity(intent)
    } else if (position == 2) {
      //TODO: uncomment for the future
      /* val dialog = new CreateGroupDialog(this)
      dialog.addCreateGroupListener(new CreateGroupListener {
        override def groupCreationConfimed(name: String): Unit = {
          val groupNumber = ToxSingleton.tox.newGroup(name)
          val groupKey = ToxSingleton.tox.getGroupKey(groupNumber)
          val db = new AntoxDB(getApplicationContext)

          db.addGroup(groupKey, name, "")
          db.close()
          ToxSingleton.updateGroupList(getApplicationContext)
        }
      })
      dialog.showDialog()
      */
      Toast.makeText(this, getResources.getString(R.string.main_group_coming_soon), Toast.LENGTH_LONG)
        .show()

    } else if (position == 3) {
      val intent = new Intent(this, classOf[AboutActivity])
      startActivity(intent)
    } else if (position == 4) {
      State.logout(this)
    }
    mDrawerList.setItemChecked(position, true)
    mDrawerLayout.closeDrawer(mDrawerList)
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

  private def setLanguage() {
    preferences = PreferenceManager.getDefaultSharedPreferences(this)

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
