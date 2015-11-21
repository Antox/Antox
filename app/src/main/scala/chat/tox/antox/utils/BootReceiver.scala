package chat.tox.antox.utils

import android.content.{BroadcastReceiver, Context, Intent}

/**
 * This Broadcast Receiver will pick up the phone booting up and will proceed to start the tox service
 */
class BootReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent): Unit = {
    // Check if autostart setting is enabled first

    //FIXME disabled autostart for now
    //val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    //if (preferences.getBoolean("autostart", true)) {
    //  context.startService(new Intent(context, classOf[ToxService]))
    //}
  }
}
