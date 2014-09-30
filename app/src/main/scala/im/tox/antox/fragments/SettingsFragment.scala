package im.tox.antox.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import java.util.Random
import im.tox.antox.R
import im.tox.antox.activities.LoginActivity
import im.tox.antox.data.AntoxDB
import im.tox.antox.data.UserDB
import im.tox.antox.tox.ToxDoService
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.Options
import im.tox.antox.utils.UserStatus
import im.tox.jtoxcore.ToxException
import im.tox.jtoxcore.ToxUserStatus
import SettingsFragment._
//remove if not needed
import scala.collection.JavaConversions._

object SettingsFragment {

  private def bindPreferenceSummaryToValue(preference: Preference) {
    preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener)
    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext)
      .getString(preference.getKey, ""))
  }

  private var sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

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
}

class SettingsFragment extends com.github.machinarius.preferencefragment.PreferenceFragment with SharedPreferences.OnSharedPreferenceChangeListener {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    var header = new PreferenceCategory(getActivity)
    addPreferencesFromResource(R.xml.pref_profile)
    header = new PreferenceCategory(getActivity)
    header.setTitle(R.string.pref_header_notifications)
    getPreferenceScreen.addPreference(header)
    addPreferencesFromResource(R.xml.pref_notification)
    header = new PreferenceCategory(getActivity)
    header.setTitle(R.string.pref_header_other)
    getPreferenceScreen.addPreference(header)
    addPreferencesFromResource(R.xml.pref_other)
    bindPreferenceSummaryToValue(findPreference("nickname"))
    bindPreferenceSummaryToValue(findPreference("status"))
    bindPreferenceSummaryToValue(findPreference("status_message"))
    bindPreferenceSummaryToValue(findPreference("language"))
    bindPreferenceSummaryToValue(findPreference("tox_id"))
    bindPreferenceSummaryToValue(findPreference("active_account"))
    val toxIDPreference = findPreference("tox_id").asInstanceOf[Preference]
    toxIDPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      override def onPreferenceClick(preference: Preference): Boolean = {
        val dialog = new DialogToxID()
        val bundle = new Bundle()
        bundle.putString("Enter Friend's Pin", "Enter Friend's Pin")
        dialog.setArguments(bundle)
        dialog.show(getFragmentManager, "NoticeDialogFragment")
        return true
      }
    })
    val logoutPreference = findPreference("logout").asInstanceOf[Preference]
    logoutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      override def onPreferenceClick(preference: Preference): Boolean = {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getActivity)
        val editor = preferences.edit()
        editor.putBoolean("loggedin", false)
        editor.apply()
        val startTox = new Intent(getActivity.getApplicationContext, classOf[ToxDoService])
        getActivity.getApplicationContext.stopService(startTox)
        val login = new Intent(getActivity.getApplicationContext, classOf[LoginActivity])
        getActivity.startActivity(login)
        getActivity.finish()
        return true
      }
    })
    val nospamPreference = findPreference("nospam").asInstanceOf[Preference]
    nospamPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      override def onPreferenceClick(preference: Preference): Boolean = {
        val toxSingleton = ToxSingleton.getInstance
        try {
          val random = new Random()
          val nospam = random.nextInt(1234567890)
          toxSingleton.jTox.setNospam(nospam)
          val preferences = PreferenceManager.getDefaultSharedPreferences(getActivity)
          val editor = preferences.edit()
          editor.putString("tox_id", toxSingleton.jTox.getAddress)
          editor.commit()
          bindPreferenceSummaryToValue(findPreference("tox_id"))
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
    val db = new UserDB(getActivity)
    if (key == "nickname") {
      val toxSingleton = ToxSingleton.getInstance
      try {
        toxSingleton.jTox.setName(sharedPreferences.getString(key, ""))
      } catch {
        case e: ToxException => e.printStackTrace()
      }
      db.updateUserDetail(sharedPreferences.getString("active_account", ""), "nickname", sharedPreferences.getString(key, 
        ""))
    }
    if (key == "status") {
      var newStatus = ToxUserStatus.TOX_USERSTATUS_NONE
      val newStatusString = sharedPreferences.getString(key, "")
      newStatus = UserStatus.getToxUserStatusFromString(newStatusString)
      val toxSingleton = ToxSingleton.getInstance
      try {
        toxSingleton.jTox.setUserStatus(newStatus)
      } catch {
        case e: ToxException => e.printStackTrace()
      }
      db.updateUserDetail(sharedPreferences.getString("active_account", ""), "status", sharedPreferences.getString(key, 
        ""))
    }
    if (key == "status_message") {
      val toxSingleton = ToxSingleton.getInstance
      try {
        toxSingleton.jTox.setStatusMessage(sharedPreferences.getString(key, ""))
      } catch {
        case e: ToxException => e.printStackTrace()
      }
      db.updateUserDetail(sharedPreferences.getString("active_account", ""), "status_message", sharedPreferences.getString(key, 
        ""))
    }
    if (key == "enable_udp") {
      val toxSingleton = ToxSingleton.getInstance
      Options.udpEnabled = sharedPreferences.getBoolean("enable_udp", false)
      val service = new Intent(getActivity, classOf[ToxDoService])
      getActivity.stopService(service)
      getActivity.startService(service)
    }
    if (key == "wifi_only") {
      val preferences = PreferenceManager.getDefaultSharedPreferences(getActivity)
      val connManager = getActivity.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
      val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
      val wifiOnly = sharedPreferences.getBoolean("wifi_only", true)
      if (wifiOnly && !mWifi.isConnected) {
        val antoxDB = new AntoxDB(getActivity)
        antoxDB.setAllOffline()
        antoxDB.close()
      }
    }
    if (key == "language") {
      val intent = getActivity.getIntent
      getActivity.finish()
      startActivity(intent)
    }
  }
}
