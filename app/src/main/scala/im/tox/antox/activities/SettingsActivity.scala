package im.tox.antox.activities

import java.util.Random

import android.content.{Context, Intent, SharedPreferences}
import android.net.ConnectivityManager
import android.os.{Build, Bundle}
import android.preference.{ListPreference, Preference, PreferenceActivity, PreferenceManager}
import android.view.MenuItem
import android.widget.Toast
import im.tox.antox.activities.Settings._
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{ToxDoService, ToxSingleton}
import im.tox.antox.utils.Options
import im.tox.antoxnightly.R
import im.tox.tox4j.exceptions.ToxException

object Settings {

  private val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

    override def onPreferenceChange(preference: Preference, value: AnyRef): Boolean = {
      val stringValue = value.toString

      preference match {
        case lp:ListPreference =>
          val index = lp.findIndexOfValue(stringValue)
          preference.setSummary(if (index >= 0) lp.getEntries()(index) else null)

        case _ =>
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
        val toxSingleton = ToxSingleton.getInstance()

        try {
          val random = new Random()
          val nospam = random.nextInt(1234567890)
          toxSingleton.tox.setNospam(nospam)
          val preferences = PreferenceManager.getDefaultSharedPreferences(Settings.this)
          val editor = preferences.edit()
          editor.putString("tox_id", toxSingleton.tox.getAddress)
          editor.apply()

          // Display toast to inform user of successful change
          Toast.makeText(
            getApplicationContext,
            getApplicationContext.getResources.getString(R.string.nospam_updated),
            Toast.LENGTH_SHORT
          ).show()

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
      val toxSingleton = ToxSingleton.getInstance()
      Options.udpEnabled = sharedPreferences.getBoolean("enable_udp", false)
      val service = new Intent(this, classOf[ToxDoService])
      this.stopService(service)
      this.startService(service)
    }
    if (key == "wifi_only") {
      if (!ToxSingleton.isToxConnected(sharedPreferences, this)) {
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
