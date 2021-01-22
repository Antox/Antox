package chat.tox.antox.fragments

import android.content.{Intent, SharedPreferences}
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.Button
import chat.tox.antox.R
import chat.tox.antox.activities.SettingsActivity
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{ConnectionManager, ConnectionTypeChangeListener}

class WifiWarningFragment extends Fragment {

  private var wifiWarningBar: Button = _

  private var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener = _

  private var preferences: SharedPreferences = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = inflater.inflate(R.layout.fragment_wifi_warning, container, false)
    wifiWarningBar = rootView.findViewById(R.id.wifi_only_warning).asInstanceOf[Button]
    wifiWarningBar.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        onClickWifiOnlyWarning(v)
      }
    })
    rootView
  }

  override def onStart(): Unit = {
    super.onStart()

    preferences = PreferenceManager.getDefaultSharedPreferences(getActivity)
    updateWifiWarning()

    ConnectionManager.addConnectionTypeChangeListener(new ConnectionTypeChangeListener {
      override def connectionTypeChange(connectionType: Int): Unit = {
        updateWifiWarning()
      }
    })

    preferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      override def onSharedPreferenceChanged(prefs: SharedPreferences, key: String): Unit = {
        key match {
          case "wifi_only" =>
            updateWifiWarning()
          case _ =>
        }
      }
    }

    preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
  }

  def updateWifiWarning(): Unit =
    if (getActivity != null) {
      if (!ToxSingleton.isToxConnected(preferences, getActivity)) {
        showWifiWarning()
      } else {
        hideWifiWarning()
      }
    }

  def onClickWifiOnlyWarning(view: View): Unit = {
    val intent = new Intent(getActivity, classOf[SettingsActivity])
    startActivity(intent)
  }

  def showWifiWarning(): Unit = {
    getView.setVisibility(View.VISIBLE)
  }

  def hideWifiWarning(): Unit = {
    getView.setVisibility(View.GONE)
  }
}