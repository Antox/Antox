package im.tox.antox.fragments

import android.content.{Context, SharedPreferences, Intent}
import android.net.ConnectivityManager
import android.os.Bundle
import android.preference.{Preference, PreferenceManager}
import android.support.v4.app.Fragment
import android.view.View.OnClickListener
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.Button
import im.tox.antox.activities.Settings
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{ConnectionTypeChangeListener, ConnectionManager}
import im.tox.antoxnightly.R

class WifiWarningFragment extends Fragment {

  var mWifiWarningBar: Button = _

  private var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener = _

  private var preferences: SharedPreferences = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = inflater.inflate(R.layout.fragment_wifi_warning, container, false)
    mWifiWarningBar = rootView.findViewById(R.id.wifi_only_warning).asInstanceOf[Button]
    mWifiWarningBar.setOnClickListener(new OnClickListener {
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

  def updateWifiWarning(): Unit = {
    if (!ToxSingleton.isToxConnected(preferences, getActivity)) {
      showWifiWarning()
    } else {
      hideWifiWarning()
    }
  }

  def onClickWifiOnlyWarning(view: View): Unit = {
    val intent = new Intent(getActivity, classOf[Settings])
    startActivity(intent)
  }

  def showWifiWarning(): Unit = {
    getView.setVisibility(View.VISIBLE)
  }

  def hideWifiWarning(): Unit = {
    getView.setVisibility(View.GONE)
  }
}