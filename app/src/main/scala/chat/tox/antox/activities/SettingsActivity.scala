package chat.tox.antox.activities

import android.content.{Intent, SharedPreferences}
import android.os.{Build, Bundle}
import android.preference.{EditTextPreference, ListPreference, Preference, PreferenceManager}
import android.view.MenuItem
import android.widget.Toast
import chat.tox.antox.R
import chat.tox.antox.activities.SettingsActivity._
import chat.tox.antox.data.State
import chat.tox.antox.fragments.ColorPickerDialog
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.{ToxService, ToxSingleton}
import chat.tox.antox.utils.{AntoxLog, AntoxNotificationManager, Options}

// import im.tox.tox4j.core.data.ToxPublicKey
import org.scaloid.common.LoggerTag

object SettingsActivity {

  private val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

    override def onPreferenceChange(preference: Preference, value: AnyRef): Boolean = {
      val stringValue = value.toString

      preference match {
        case lp: ListPreference =>
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

class SettingsActivity extends BetterPreferenceActivity with Preference.OnPreferenceClickListener {

  private var themeDialog: ColorPickerDialog = _
  private var thisActivity: SettingsActivity = _

  override def onCreate(savedInstanceState: Bundle) {
    thisActivity = this

    getDelegate.installViewFactory()
    getDelegate.onCreate(savedInstanceState)
    super.onCreate(savedInstanceState)

    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    ThemeManager.applyTheme(this, getSupportActionBar)

    themeDialog = new ColorPickerDialog(this, new ColorPickerDialog.Callback {
      override def onColorSelection(index: Int, color: Int, darker: Int): Unit = {
        ThemeManager.primaryColor = color
        ThemeManager.primaryColorDark = darker

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          // it's a shame this can't be
          // used to recreate this activity and still change the theme
          val i = new Intent(getApplicationContext, classOf[MainActivity])
          i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          finish()
          startActivity(i)
        }
      }
    })

    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean("showing_theme_dialog", false)) showThemeDialog()
    }

    addPreferencesFromResource(R.xml.settings_main)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
      getActionBar != null) {
      getActionBar.setDisplayHomeAsUpEnabled(true)
    }

    findPreference("theme_color").setOnPreferenceClickListener(this)
    findPreference("call_replies").setOnPreferenceClickListener(this)
    bindPreferenceSummaryToValue(findPreference("locale"))

    bindPreferenceSummaryToValue(findPreference("proxy_type"))
    bindPreferenceSummaryToValue(findPreference("proxy_address"))
    bindPreferenceSummaryToValue(findPreference("proxy_port"))

    bindPreferenceSummaryToValue(findPreference("custom_node_address"))
    bindPreferenceSummaryToValue(findPreference("custom_node_port"))
    bindPreferenceSummaryToValue(findPreference("custom_node_key"))

  }

  override def onResume() {
    super.onResume()
    getPreferenceScreen.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override def onPause() {
    super.onPause()
    getPreferenceScreen.getSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }

  override def onStop() {
    super.onStop()
    themeDialog.close()
  }

  override def onSaveInstanceState(savedInstanceState: Bundle): Unit = {
    super.onSaveInstanceState(savedInstanceState)

    // this is needed to keep the theme dialog open on rotation
    // the hack is required because PreferenceActivity doesn't allow for dialog fragments
    savedInstanceState.putBoolean("showing_theme_dialog", themeDialog.isShowing)
  }

  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    var networkSettingsChanged = false
    val proxySettings = List("enable_proxy", "proxy_type", "proxy_address", "proxy_port")
    if (key == "enable_udp") {
      Options.udpEnabled = sharedPreferences.getBoolean("enable_udp", false)
      networkSettingsChanged = true
    }
    if (proxySettings.contains(key)) {
      networkSettingsChanged = true
    }

    if (sharedPreferences.getBoolean("autoacceptft", false) == true) {
      State.setAutoAcceptFt(true)
    }
    else {
      State.setAutoAcceptFt(false)
    }

    if (sharedPreferences.getBoolean("batterysavingmode", false) == true) {
      State.setBatterySavingMode(true)
    }
    else {
      State.setBatterySavingMode(false)
    }


    if (sharedPreferences.getBoolean("videocallstartwithnovideo", false) == true) {
      Options.videoCallStartWithNoVideo = true
    }
    else {
      Options.videoCallStartWithNoVideo = false
    }


    if (key == "proxy_address") {
      val address = sharedPreferences.getString("proxy_address", "127.0.0.1")
      if (!address.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
        Toast.makeText(getApplicationContext, getString(R.string.error_invalid_ip_address), Toast.LENGTH_SHORT).show()
        val preference = findPreference("proxy_address").asInstanceOf[EditTextPreference]
        preference.setText("127.0.0.1")
        preference.setSummary("127.0.0.1")
        networkSettingsChanged = false
      }
    }
    if (key == "proxy_port") {
      val port = sharedPreferences.getString("proxy_port", "9050").toInt
      if (!(port > 0 && port < 65535)) {
        Toast.makeText(getApplicationContext, getString(R.string.error_invalid_port), Toast.LENGTH_SHORT).show()
        val preference = findPreference("proxy_port").asInstanceOf[EditTextPreference]
        preference.setText("9050")
        preference.setSummary("9050")
        networkSettingsChanged = false
      }
    }

    if (key == "custom_node_address") {
      val address = sharedPreferences.getString("custom_node_address", "127.0.0.1")
      if (!address.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
        Toast.makeText(getApplicationContext, getString(R.string.error_invalid_ip_address), Toast.LENGTH_SHORT).show()
        val preference = findPreference("custom_node_address").asInstanceOf[EditTextPreference]
        preference.setText("127.0.0.1")
        preference.setSummary("127.0.0.1")
      }
    }

    if (key == "custom_node_port") {
      val port = sharedPreferences.getString("custom_node_port", "33445").toInt
      if (!(port > 0 && port < 65535)) {
        Toast.makeText(getApplicationContext, getString(R.string.error_invalid_port), Toast.LENGTH_SHORT).show()
        val preference = findPreference("custom_node_port").asInstanceOf[EditTextPreference]
        preference.setText("33445")
        preference.setSummary("33445")
      }
    }

    if (key == "custom_node_key") {
      val address = sharedPreferences.getString("custom_node_key", "")
      if (address.length != 64 || !address.matches("^[0-9A-F]+$")) {
        AntoxLog.error("Malformed tox public key", LoggerTag("SettingsActivity"))
        Toast.makeText(getApplicationContext, getString(R.string.error_invalid_tox_id), Toast.LENGTH_SHORT).show()
        val preference = findPreference("custom_node_key").asInstanceOf[EditTextPreference]
        preference.setText("")
        preference.setSummary("")
      }
    }

    if (key == "wifi_only") {
      if (!ToxSingleton.isToxConnected(sharedPreferences, this)) {
        val antoxDb = State.db
        antoxDb.setAllOffline()
      }
    }
    if (key == "locale") {
      val intent = new Intent(getApplicationContext, classOf[MainActivity])
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      finish()
      startActivity(intent)
    }
    if (key == "notifications_persistent") {
      if (sharedPreferences.getBoolean("notifications_persistent", false) &&
        sharedPreferences.getBoolean("notifications_enable_notifications", true)) {
        AntoxNotificationManager.createPersistentNotification(getApplicationContext)
      } else {
        AntoxNotificationManager.removePersistentNotification()
      }
    }
    if (key == "notifications_enable_notifications") {
      if (sharedPreferences.getBoolean("notifications_persistent", false) &&
        sharedPreferences.getBoolean("notifications_enable_notifications", true)) {
        AntoxNotificationManager.createPersistentNotification(getApplicationContext)
      } else {
        AntoxNotificationManager.removePersistentNotification()
      }
    }

    if (networkSettingsChanged) {
      AntoxLog.debug("One or more network settings changed. Restarting Tox service")
      val service = new Intent(this, classOf[ToxService])
      this.stopService(service)
      this.startService(service)
    }
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case android.R.id.home =>
      finish()
      true
  }

  override def onPreferenceClick(preference: Preference): Boolean = {
    preference.getKey match {
      case "theme_color" =>
        showThemeDialog()
      case "call_replies" =>
        launchCallRepliesActivity()
      case _ => //do nothing
    }

    true
  }

  def showThemeDialog(): Unit = {
    val currentColor = ThemeManager.primaryColor
    themeDialog.show(currentColor match {
      case -1 => None
      case _ => Some(currentColor)
    })
  }

  def launchCallRepliesActivity(): Unit = {
    val intent = new Intent(this, classOf[EditCallRepliesActivity])
    startActivity(intent)
  }
}
