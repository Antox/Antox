package im.tox.antox.activities

import java.util.Random

import android.content.{Context, Intent, SharedPreferences}
import android.net.ConnectivityManager
import android.os.{Build, Bundle}
import android.preference.{ListPreference, Preference, PreferenceActivity, PreferenceManager}
import android.view.MenuItem
import im.tox.antox.R
import im.tox.antox.activities.Settings._
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{ToxDoService, ToxSingleton}
import im.tox.antox.utils.Options
import im.tox.jtoxcore.ToxException
//remove if not needed

object Settings {

  private val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

    override def onPreferenceChange(preference: Preference, value: AnyRef): Boolean = {
      val stringValue = value.toString
      if (preference.isInstanceOf[ListPreference]) {
        val listPreference = preference.asInstanceOf[ListPreference]
        val index = listPreference.findIndexOfValue(stringValue)
        preference.setSummary(if (index >= 0) listPreference.getEntries()(index) else null)
      } else {
        preference.setSummary(stringValue)
      }
      true
    }
  }

  private def bindPreferenceSummaryToValue(preference: Preference) {
    preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener)
    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext)
      .getString(preference.getKey, ""))
  }
}

class Settings extends PreferenceActivity with SharedPreferences.OnSharedPreferenceChangeListener {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.settings_main)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
      getActionBar != null) {
      getActionBar.setDisplayHomeAsUpEnabled(true)
    }
    bindPreferenceSummaryToValue(findPreference("language"))
    val nospamPreference = findPreference("nospam")
    nospamPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      override def onPreferenceClick(preference: Preference): Boolean = {
        val toxSingleton = ToxSingleton.getInstance
        try {
          val random = new Random()
          val nospam = random.nextInt(1234567890)
          toxSingleton.jTox.setNospam(nospam)
          val preferences = PreferenceManager.getDefaultSharedPreferences(Settings.this)
          val editor = preferences.edit()
          editor.putString("tox_id", toxSingleton.jTox.getAddress)
          editor.apply()
        } catch {
          case e: ToxException => e.printStackTrace()
        }
        true
      }
    })
  }

  override def onResume() {
    super.onResume()
    getPreferenceScreen.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override def onPause() {
    super.onPause()
    getPreferenceScreen.getSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key == "enable_udp") {
      val toxSingleton = ToxSingleton.getInstance
      Options.udpEnabled = sharedPreferences.getBoolean("enable_udp", false)
      val service = new Intent(this, classOf[ToxDoService])
      this.stopService(service)
      this.startService(service)
    }
    if (key == "wifi_only") {
      val preferences = PreferenceManager.getDefaultSharedPreferences(this)
      val connManager = this.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
      val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
      val wifiOnly = sharedPreferences.getBoolean("wifi_only", true)
      if (wifiOnly && !mWifi.isConnected) {
        val antoxDB = new AntoxDB(this)
        antoxDB.setAllOffline()
        antoxDB.close()
      }
    }
    if (key == "language") {
      val intent = getBaseContext.getPackageManager.getLaunchIntentForPackage(getBaseContext.getPackageName)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      startActivity(intent)
    }
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case android.R.id.home =>
      finish()
      true

  }
}
