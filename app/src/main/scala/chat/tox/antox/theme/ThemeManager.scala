package chat.tox.antox.theme

import android.app.Activity
import android.content.{Context, SharedPreferences}
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.preference.PreferenceManager
import android.support.v7.app.ActionBar
import chat.tox.antox.R

object ThemeManager {

  private var _primaryColor: Int = _
  var _primaryColorDark: Int = _

  private var preferences: SharedPreferences = _

  def init(context: Context): Unit = {
    preferences = PreferenceManager.getDefaultSharedPreferences(context)
    _primaryColor = preferences.getInt("theme_color", context.getResources.getColor(R.color.brand_primary))
    _primaryColorDark = darkenColor(_primaryColor)
  }

  def darkenColor(color: Int): Int = {
    val hsv: Array[Float] = new Array[Float](3)
    Color.colorToHSV(color, hsv)
    hsv(2) *= 0.9f
    Color.HSVToColor(hsv)
  }

  def applyTheme(activity: Activity, actionBar: ActionBar): Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && actionBar != null) {
      actionBar.setBackgroundDrawable(new ColorDrawable(ThemeManager.primaryColor))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      activity.getWindow.setStatusBarColor(ThemeManager.primaryColorDark)
    }
  }

  //getters
  def primaryColor: Int = _primaryColor
  def primaryColorDark: Int = _primaryColorDark

  //setters
  def primaryColor_=(primaryColor: Int): Unit = {
    _primaryColor = primaryColor
    preferences.edit.putInt("theme_color", primaryColor).apply()
  }

  def primaryColorDark_=(primaryColorDark: Int): Unit = {
    _primaryColorDark = primaryColorDark
  }
}
