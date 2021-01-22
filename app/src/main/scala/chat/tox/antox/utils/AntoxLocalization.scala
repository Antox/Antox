package chat.tox.antox.utils

import java.util.Locale

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager


object AntoxLocalization {

  def setLanguage(context: Context) = {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val localeString = preferences.getString("locale", "-1")

    val locale = context.getResources.getConfiguration.locale

    if (localeString == "-1") {
      val editor = preferences.edit()
      val currentLanguage = locale.getLanguage.toLowerCase
      val currentCountry = locale.getCountry

      editor.putString("locale", currentLanguage + "_" + currentCountry)
      editor.apply()
      context.getResources.getConfiguration
    }
    else {
      val locale = if (localeString.contains("_")) {
        val (language, country) = localeString.splitAt(localeString.indexOf("_"))
        new Locale(language, country)
      } else {
        new Locale(localeString)
      }

      AntoxLog.debug(s"Setting locale: $localeString")
      Locale.setDefault(locale)
      val config = context.getResources.getConfiguration

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        config.setLocale(locale)
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createConfigurationContext(config)
      } else {
        context.getResources.updateConfiguration(config, context.getResources.getDisplayMetrics)
      }

    }
  }
}
