package im.tox.antox.utils

import android.content.{BroadcastReceiver, Context, Intent}
import android.net.ConnectivityManager
import im.tox.antox.tox.ToxSingleton
//remove if not needed

class ConnectionChangeReceiver extends BroadcastReceiver {

  override def onReceive(context: Context, intent: Intent) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val networkInfo = connectivityManager.getActiveNetworkInfo
    if (networkInfo != null && networkInfo.isConnected) {
      if (ToxSingleton.dhtNodes.size == 0) {
        ToxSingleton.updateDhtNodes(context)
      }
    }
  }
}
