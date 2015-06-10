package im.tox.antox.utils

import android.content.{BroadcastReceiver, Context, Intent}
import android.preference.PreferenceManager
import im.tox.antox.tox.ToxService

/**
 * This Broadcast Receiver will pick up the phone booting up and will proceed to start the tox service
 */
class BootReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent): Unit = {
    /* Check if autostart setting is enabled first */
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    if (preferences.getBoolean("autostart", true))
      context.startService(new Intent(context, classOf[ToxService]))
  }
}
